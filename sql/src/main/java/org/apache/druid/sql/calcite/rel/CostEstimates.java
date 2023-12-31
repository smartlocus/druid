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

package org.apache.druid.sql.calcite.rel;

import org.apache.calcite.plan.RelOptPlanner;
import org.apache.calcite.rel.metadata.RelMetadataQuery;

/**
 * Constants used by {@link PartialDruidQuery#estimateCost} and various
 * {@link DruidRel#computeSelfCost(RelOptPlanner, RelMetadataQuery)} implementations.
 */
public class CostEstimates
{
  /**
   * Per-row base cost. This represents the cost of walking through every row, but not actually reading anything
   * from them or computing any aggregations.
   */
  public static final double COST_BASE = 1;

  /**
   * Cost to include a column in query output.
   */
  public static final double COST_OUTPUT_COLUMN = 0.05;

  /**
   * Cost to compute and read an expression.
   */
  public static final double COST_EXPRESSION = 0.25;

  /**
   * Cost to compute an aggregation.
   */
  public static final double COST_AGGREGATION = 0.05;

  /**
   * Cost per GROUP BY dimension.
   */
  public static final double COST_DIMENSION = 0.25;

  /**
   * Multiplier to apply when there is a WHERE filter. Encourages pushing down filters and limits through joins and
   * subqueries when possible.
   */
  public static final double MULTIPLIER_FILTER = 0.1;

  /**
   * Multiplier to apply when there is an ORDER BY. Encourages avoiding them when possible.
   */
  public static final double MULTIPLIER_ORDER_BY = 10;

  /**
   * Multiplier to apply when there is a LIMIT. Encourages pushing down limits when possible.
   */
  public static final double MULTIPLIER_LIMIT = 0.5;

  /**
   * Multiplier to apply to an outer query via {@link DruidOuterQueryRel}. Encourages pushing down time-saving
   * operations to the lowest level of the query stack, because they'll have bigger impact there.
   */
  public static final double MULTIPLIER_OUTER_QUERY = .1;

  /**
   * Cost to add to a subquery. Strongly encourages avoiding subqueries, since they must be inlined and then the join
   * must run on the Broker.
   */
  public static final double COST_SUBQUERY = 1e5;

  /**
   * Cost to perform a cross join. Strongly encourages pushing down filters into join conditions, even if it means
   * we need to add a subquery (this is higher than {@link #COST_SUBQUERY}).
   */
  public static final double COST_JOIN_CROSS = 1e8;

  private CostEstimates()
  {
    // No instantiation.
  }
}
