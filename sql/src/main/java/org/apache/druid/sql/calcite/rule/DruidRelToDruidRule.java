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

package org.apache.druid.sql.calcite.rule;

import org.apache.calcite.plan.Convention;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.convert.ConverterRule;
import org.apache.druid.java.util.common.logger.Logger;
import org.apache.druid.sql.calcite.rel.DruidConvention;
import org.apache.druid.sql.calcite.rel.DruidRel;

import javax.annotation.Nullable;

public class DruidRelToDruidRule extends ConverterRule
{
  private static final Logger log = new Logger(DruidRelToDruidRule.class);
  private static final DruidRelToDruidRule INSTANCE = new DruidRelToDruidRule();

  private DruidRelToDruidRule()
  {
    super(
        DruidRel.class,
        Convention.NONE,
        DruidConvention.instance(),
        DruidRelToDruidRule.class.getSimpleName()
    );
  }

  public static DruidRelToDruidRule instance()
  {
    return INSTANCE;
  }

  @Nullable
  @Override
  public RelNode convert(RelNode rel)
  {
    try {
      return ((DruidRel<?>) rel).asDruidConvention();
    }
    catch (Exception e) {
      log.error(e, "Conversion failed");
      throw e;
    }
  }
}
