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

package org.apache.druid.data.input.parquet;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.druid.data.input.InputEntity;
import org.apache.druid.data.input.InputEntityReader;
import org.apache.druid.data.input.InputRowSchema;
import org.apache.druid.data.input.impl.NestedInputFormat;
import org.apache.druid.data.input.parquet.guice.Parquet;
import org.apache.druid.java.util.common.parsers.JSONPathSpec;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class ParquetInputFormat extends NestedInputFormat
{
  static final long SCALE_FACTOR = 8L;
  private final boolean binaryAsString;
  private final Configuration conf;

  @JsonCreator
  public ParquetInputFormat(
      @JsonProperty("flattenSpec") @Nullable JSONPathSpec flattenSpec,
      @JsonProperty("binaryAsString") @Nullable Boolean binaryAsString,
      @JacksonInject @Parquet Configuration conf
  )
  {
    super(flattenSpec);
    this.binaryAsString = binaryAsString == null ? false : binaryAsString;
    this.conf = conf;
  }

  private void initialize(Configuration conf)
  {
    // Initializing seperately since during eager initialization, resolving
    // namenode hostname throws an error if nodes are ephemeral

    // Ensure that FileSystem class level initialization happens with correct CL
    // See https://github.com/apache/druid/issues/1714
    ClassLoader currCtxCl = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
      FileSystem.get(conf);
    }
    catch (IOException ex) {
      throw new RuntimeException(ex);
    }
    finally {
      Thread.currentThread().setContextClassLoader(currCtxCl);
    }
  }

  @JsonProperty
  @JsonInclude(JsonInclude.Include.NON_DEFAULT)
  public boolean getBinaryAsString()
  {
    return binaryAsString;
  }

  @Override
  public boolean isSplittable()
  {
    return false;
  }

  @Override
  public InputEntityReader createReader(
      InputRowSchema inputRowSchema,
      InputEntity source,
      File temporaryDirectory
  )
  {
    initialize(conf);
    return new ParquetReader(conf, inputRowSchema, source, temporaryDirectory, getFlattenSpec(), binaryAsString);
  }

  @Override
  public long getWeightedSize(String path, long size)
  {
    return size * SCALE_FACTOR;
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
    ParquetInputFormat that = (ParquetInputFormat) o;
    return binaryAsString == that.binaryAsString;
  }

  @Override
  public int hashCode()
  {
    return Objects.hash(super.hashCode(), binaryAsString);
  }
}
