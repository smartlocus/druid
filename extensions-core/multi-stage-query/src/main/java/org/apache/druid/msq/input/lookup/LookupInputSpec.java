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

package org.apache.druid.msq.input.lookup;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import org.apache.druid.msq.input.InputSpec;

import java.util.Objects;

/**
 * Represents a lookup table. Corresponds to {@link org.apache.druid.query.LookupDataSource}.
 */
public class LookupInputSpec implements InputSpec
{
  private final String lookupName;

  @JsonCreator
  public LookupInputSpec(@JsonProperty("lookupName") final String lookupName)
  {
    this.lookupName = Preconditions.checkNotNull(lookupName, "lookupName");
  }

  @JsonProperty
  public String getLookupName()
  {
    return lookupName;
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
    LookupInputSpec that = (LookupInputSpec) o;
    return Objects.equals(lookupName, that.lookupName);
  }

  @Override
  public int hashCode()
  {
    return Objects.hash(lookupName);
  }

  @Override
  public String toString()
  {
    return "LookupInputSpec{" +
           "lookupName='" + lookupName + '\'' +
           '}';
  }
}
