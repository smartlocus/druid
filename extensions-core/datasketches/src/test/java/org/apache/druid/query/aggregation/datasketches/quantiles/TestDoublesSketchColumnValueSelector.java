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

package org.apache.druid.query.aggregation.datasketches.quantiles;

import org.apache.datasketches.quantiles.DoublesSketch;
import org.apache.druid.query.monomorphicprocessing.RuntimeShapeInspector;
import org.apache.druid.segment.ColumnValueSelector;

import javax.annotation.Nullable;

public class TestDoublesSketchColumnValueSelector implements ColumnValueSelector<DoublesSketch>
{
  @Override
  public void inspectRuntimeShape(RuntimeShapeInspector inspector)
  {
  }

  @Override
  public double getDouble()
  {
    return 99;
  }

  @Override
  public float getFloat()
  {
    return 99;
  }

  @Override
  public long getLong()
  {
    return 99;
  }

  @Override
  public boolean isNull()
  {
    return false;
  }

  @Nullable
  @Override
  public DoublesSketch getObject()
  {
    return DoublesSketchOperations.EMPTY_SKETCH;
  }

  @Override
  public Class<? extends DoublesSketch> classOfObject()
  {
    return DoublesSketchOperations.EMPTY_SKETCH.getClass();
  }
}
