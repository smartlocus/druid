/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.druid.emitter.ambari.metrics;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.druid.java.util.common.ISE;
import org.apache.druid.java.util.common.StringUtils;
import org.apache.druid.java.util.common.logger.Logger;
import org.apache.druid.java.util.emitter.core.Emitter;
import org.apache.druid.java.util.emitter.core.Event;
import org.apache.druid.java.util.emitter.service.AlertEvent;
import org.apache.druid.java.util.emitter.service.SegmentMetadataEvent;
import org.apache.druid.java.util.emitter.service.ServiceMetricEvent;
import org.apache.hadoop.metrics2.sink.timeline.AbstractTimelineMetricsSink;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;


public class AmbariMetricsEmitter extends AbstractTimelineMetricsSink implements Emitter
{
  private static final Logger log = new Logger(AmbariMetricsEmitter.class);

  private final DruidToTimelineMetricConverter timelineMetricConverter;
  private final List<Emitter> emitterList;
  private final AtomicBoolean started = new AtomicBoolean(false);
  private final LinkedBlockingQueue<TimelineMetric> eventsQueue;
  private final AmbariMetricsEmitterConfig config;
  private final String collectorURI;
  private static final long DEFAULT_FLUSH_TIMEOUT_MILLIS = 60000; // default flush wait 1 min
  private static final Pattern DOT_OR_WHITESPACE_PATTERN = Pattern.compile("[\\s]+|[.]+");
  private final ScheduledExecutorService exec = Executors.newScheduledThreadPool(
      2, // Thread pool of two in order to schedule flush runnable
      new ThreadFactoryBuilder().setDaemon(true).setNameFormat("AmbariMetricsEmitter-%s").build()
  );
  private final AtomicLong countLostEvents = new AtomicLong(0);

  public AmbariMetricsEmitter(
      AmbariMetricsEmitterConfig config,
      List<Emitter> emitterList
  )
  {
    this.config = config;
    this.emitterList = emitterList;
    this.timelineMetricConverter = config.getDruidToTimelineEventConverter();
    this.eventsQueue = new LinkedBlockingQueue<>(config.getMaxQueueSize());
    this.collectorURI = StringUtils.format(
        "%s://%s:%s%s",
        config.getProtocol(),
        config.getHostname(),
        config.getPort(),
        WS_V1_TIMELINE_METRICS
    );
  }

  @Override
  public void start()
  {
    synchronized (started) {
      log.info("Starting Ambari Metrics Emitter.");
      if (!started.get()) {
        if ("https".equals(config.getProtocol())) {
          loadTruststore(config.getTrustStorePath(), config.getTrustStoreType(), config.getTrustStorePassword());
        }
        exec.scheduleAtFixedRate(
            new ConsumerRunnable(),
            config.getFlushPeriod(),
            config.getFlushPeriod(),
            TimeUnit.MILLISECONDS
        );
        started.set(true);
      }
    }
  }


  @Override
  public void emit(Event event)
  {
    if (!started.get()) {
      throw new ISE("Emit called unexpectedly before service start");
    }
    if (event instanceof ServiceMetricEvent) {
      final TimelineMetric timelineEvent = timelineMetricConverter.druidEventToTimelineMetric((ServiceMetricEvent) event);
      if (timelineEvent == null) {
        return;
      }
      try {
        final boolean isSuccessful = eventsQueue.offer(
            timelineEvent,
            config.getEmitWaitTime(),
            TimeUnit.MILLISECONDS
        );
        if (!isSuccessful) {
          if (countLostEvents.getAndIncrement() % 1000 == 0) {
            log.error(
                "Lost total of [%s] events because of emitter queue is full. Please increase the capacity or/and the consumer frequency",
                countLostEvents.get()
            );
          }
        }
      }
      catch (InterruptedException e) {
        log.error(e, "got interrupted with message [%s]", e.getMessage());
        Thread.currentThread().interrupt();
      }
    } else if (event instanceof AlertEvent) {
      for (Emitter emitter : emitterList) {
        emitter.emit(event);
      }
    } else if (event instanceof SegmentMetadataEvent) {
      // do nothing. Ignore this event type
    } else {
      throw new ISE("unknown event type [%s]", event.getClass());
    }
  }

  @Override
  protected String getCollectorUri(String host)
  {
    return constructTimelineMetricUri(getCollectorProtocol(), host, getCollectorPort());
  }

  @Override
  protected String getCollectorProtocol()
  {
    return config.getProtocol();
  }

  @Override
  protected String getCollectorPort()
  {
    return String.valueOf(config.getPort());
  }

  @Override
  protected int getTimeoutSeconds()
  {
    return (int) (DEFAULT_FLUSH_TIMEOUT_MILLIS / 1000);
  }

  @Override
  protected String getZookeeperQuorum()
  {
    //Ignoring Zk Fallback.
    return null;
  }

  @Override
  protected Collection<String> getConfiguredCollectorHosts()
  {
    return Collections.singleton(config.getHostname());
  }

  @Override
  protected String getHostname()
  {
    return config.getHostname();
  }

  @Override
  protected boolean isHostInMemoryAggregationEnabled()
  {
    return false;
  }

  @Override
  protected int getHostInMemoryAggregationPort()
  {
    return 0;  // since host in-memory aggregation is disabled, this return value is unimportant
  }

  @Override
  protected String getHostInMemoryAggregationProtocol()
  {
    return "";  // since host in-memory aggregation is disabled, this return value is unimportant
  }

  private class ConsumerRunnable implements Runnable
  {
    @Override
    public void run()
    {
      try {
        int batchSize = config.getBatchSize();
        TimelineMetrics metrics = new TimelineMetrics();
        while (eventsQueue.size() > 0 && !exec.isShutdown()) {
          try {
            final TimelineMetric metricEvent = eventsQueue.poll(
                config.getWaitForEventTime(),
                TimeUnit.MILLISECONDS
            );
            if (metricEvent != null) {
              metrics.addOrMergeTimelineMetric(metricEvent);
              if (metrics.getMetrics().size() == batchSize) {
                emitMetrics(metrics);
                log.debug(
                    "sent [%d] events",
                    metrics.getMetrics().size()
                );
                metrics = new TimelineMetrics();
              }
            }
          }
          catch (InterruptedException e) {
            log.error(e, e.getMessage());
            Thread.currentThread().interrupt();
          }
        }
        if (metrics.getMetrics().size() > 0) {
          emitMetrics(metrics);
          log.debug(
              "sent [%d] events",
              metrics.getMetrics().size()
          );
        }
      }
      catch (Exception e) {
        log.error(e, e.getMessage());
      }

    }
  }

  @Override
  public void flush()
  {
    synchronized (started) {
      if (started.get()) {
        Future future = exec.schedule(new ConsumerRunnable(), 0, TimeUnit.MILLISECONDS);
        try {
          future.get(DEFAULT_FLUSH_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        }
        catch (InterruptedException | ExecutionException | TimeoutException e) {
          if (e instanceof InterruptedException) {
            throw new RuntimeException("interrupted flushing elements from queue", e);
          }
        }
      }
    }
  }

  @Override
  public void close()
  {
    synchronized (started) {
      flush();
      exec.shutdown();
      started.set(false);
    }
  }

  protected static String sanitize(String namespace)
  {
    return DOT_OR_WHITESPACE_PATTERN.matcher(namespace).replaceAll("_");
  }
}
