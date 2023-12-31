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

package org.apache.druid.indexing.input;

import org.apache.druid.data.input.InputSource;
import org.apache.druid.indexing.common.TaskToolbox;

/**
 * An InputSource that allows setting a {@link TaskToolbox}
 * to be used for various purposes such as submitting task actions to the Overlord.
 */
public interface TaskInputSource extends InputSource
{
  /**
   * Creates and returns a new {@code InputSource} which uses the given {@code TaskToolbox}.
   */
  InputSource withTaskToolbox(TaskToolbox toolbox);
}
