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

package org.apache.druid.query.filter;

/**
 * Note: this is not a {@link org.apache.druid.guice.annotations.PublicApi} or an
 * {@link org.apache.druid.guice.annotations.ExtensionPoint} of Druid.
 */
// All implementations are currently lambda expressions and intellij inspections wrongly complains about unused
// variable. SupressWarnings can be removed once https://youtrack.jetbrains.com/issue/IDEA-191743 is resolved.
@SuppressWarnings("unused")
public interface DruidFloatPredicate
{
  DruidFloatPredicate ALWAYS_FALSE = input -> false;

  DruidFloatPredicate ALWAYS_TRUE = input -> true;

  DruidFloatPredicate MATCH_NULL_ONLY = new DruidFloatPredicate()
  {
    @Override
    public boolean applyFloat(float input)
    {
      return false;
    }

    @Override
    public boolean applyNull()
    {
      return true;
    }
  };


  boolean applyFloat(float input);

  default boolean applyNull()
  {
    return false;
  }
}
