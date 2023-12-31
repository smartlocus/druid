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

package org.apache.druid.guice;

import com.google.common.base.Supplier;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provider;


/**
 * A Provider of a Supplier that uses a Provider to implement the Supplier.
 */
public class ProviderBasedGoogleSupplierProvider<T> implements Provider<Supplier<T>>
{
  private final Key<T> supplierKey;
  private Provider<T> instanceProvider;

  public ProviderBasedGoogleSupplierProvider(
      Key<T> instanceKey
  )
  {
    this.supplierKey = instanceKey;
  }

  @Inject
  public void configure(Injector injector)
  {
    this.instanceProvider = injector.getProvider(supplierKey);
  }


  @Override
  public Supplier<T> get()
  {
    return instanceProvider::get;
  }
}
