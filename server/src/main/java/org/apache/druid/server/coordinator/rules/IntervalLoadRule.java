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

package org.apache.druid.server.coordinator.rules;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.druid.java.util.common.logger.Logger;
import org.apache.druid.timeline.DataSegment;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;

/**
 */
public class IntervalLoadRule extends LoadRule
{
  private static final Logger log = new Logger(IntervalLoadRule.class);

  private final Interval interval;

  @JsonCreator
  public IntervalLoadRule(
      @JsonProperty("interval") Interval interval,
      @JsonProperty("tieredReplicants") Map<String, Integer> tieredReplicants,
      @JsonProperty("useDefaultTierForNull") @Nullable Boolean useDefaultTierForNull
  )
  {
    super(tieredReplicants, useDefaultTierForNull);
    this.interval = interval;
  }

  @Override
  @JsonProperty
  public String getType()
  {
    return "loadByInterval";
  }

  @JsonProperty
  public Interval getInterval()
  {
    return interval;
  }

  @Override
  public boolean appliesTo(DataSegment segment, DateTime referenceTimestamp)
  {
    return appliesTo(segment.getInterval(), referenceTimestamp);
  }

  @Override
  public boolean appliesTo(Interval theInterval, DateTime referenceTimestamp)
  {
    return Rules.eligibleForLoad(interval, theInterval);
  }

  @Override
  public boolean equals(Object o)
  {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    IntervalLoadRule that = (IntervalLoadRule) o;
    return Objects.equals(interval, that.interval);
  }

  @Override
  public int hashCode()
  {
    return Objects.hash(super.hashCode(), interval);
  }
}
