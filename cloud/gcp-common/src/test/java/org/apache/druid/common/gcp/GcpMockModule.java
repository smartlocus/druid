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

package org.apache.druid.common.gcp;

import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.testing.http.MockHttpTransport;
import com.google.inject.Binder;
import com.google.inject.Provides;
import org.apache.druid.guice.LazySingleton;
import org.apache.druid.initialization.DruidModule;

public abstract class GcpMockModule implements DruidModule
{
  @Override
  public void configure(Binder binder)
  {
  }

  @Provides
  @LazySingleton
  public HttpRequestInitializer provideRequestInitializer(
      HttpTransport transport,
      JsonFactory factory
  )
  {
    return mockRequestInitializer(transport, factory);
  }

  public abstract HttpRequestInitializer mockRequestInitializer(
      HttpTransport transport,
      JsonFactory factory
  );

  @Provides
  @LazySingleton
  public HttpTransport buildMockTransport()
  {
    return new MockHttpTransport.Builder().build();
  }

  @Provides
  @LazySingleton
  public JsonFactory getJsonFactory()
  {
    return JacksonFactory.getDefaultInstance();
  }
}
