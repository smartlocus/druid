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

package org.apache.druid.query.rowsandcols.semantic;

import org.apache.druid.java.util.common.ISE;
import org.apache.druid.query.rowsandcols.RowsAndColumns;

public interface WireTransferable
{
  static WireTransferable fromRAC(RowsAndColumns rac)
  {
    WireTransferable retVal = rac.as(WireTransferable.class);
    if (retVal == null) {
      throw new ISE("Rac[%s] cannot be transferred over the wire", rac.getClass());
    }
    return retVal;
  }

  byte[] bytesToTransfer();
}
