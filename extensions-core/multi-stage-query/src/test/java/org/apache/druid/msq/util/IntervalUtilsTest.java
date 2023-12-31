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

package org.apache.druid.msq.util;

import org.apache.druid.java.util.common.DateTimes;
import org.apache.druid.java.util.common.Intervals;
import org.apache.druid.java.util.common.granularity.Granularities;
import org.apache.druid.java.util.common.granularity.PeriodGranularity;
import org.joda.time.Interval;
import org.joda.time.Period;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class IntervalUtilsTest
{
  @Test
  public void test_difference()
  {
    Assert.assertEquals(
        intervals(),
        IntervalUtils.difference(intervals(), intervals("2000/P1D"))
    );

    Assert.assertEquals(
        intervals("2000/P1D"),
        IntervalUtils.difference(intervals("2000/P1D"), intervals())
    );

    Assert.assertEquals(
        intervals("2000/2001"),
        IntervalUtils.difference(intervals("2000/2001"), intervals("2003/2004"))
    );

    Assert.assertEquals(
        intervals("2000-01-02/2001"),
        IntervalUtils.difference(intervals("2000/2001"), intervals("2000/P1D"))
    );

    Assert.assertEquals(
        intervals("2000/2000-02-01", "2000-02-02/2001"),
        IntervalUtils.difference(intervals("2000/2001"), intervals("2000-02-01/P1D"))
    );

    Assert.assertEquals(
        intervals(),
        IntervalUtils.difference(intervals("2000/2001"), intervals("1999/2001"))
    );

    Assert.assertEquals(
        intervals("2000-01-14/2000-02-01", "2000-02-02/2001"),
        IntervalUtils.difference(intervals("2000/P1D", "2000-01-14/2001"), intervals("2000/P1D", "2000-02-01/P1D"))
    );

    Assert.assertEquals(
        intervals("2000-01-01/2000-07-01", "2000-07-02/2001-01-01", "2002-01-01/2002-07-01", "2002-07-02/2003-01-01"),
        IntervalUtils.difference(intervals("2000/P1Y", "2002/P1Y"), intervals("2000-07-01/P1D", "2002-07-01/P1D"))
    );

    Assert.assertEquals(
        intervals(),
        IntervalUtils.difference(intervals("2000-01-12/2000-01-15"), intervals("2000-01-12/2000-01-13", "2000-01-13/2000-01-16"))
    );

    Assert.assertEquals(
        intervals("2000-07-14/2000-07-15"),
        IntervalUtils.difference(intervals("2000/2001"), intervals("2000-01-01/2000-07-14", "2000-07-15/2001"))
    );
  }

  @Test
  public void test_doesIntervalMatchesGranularity_withStandardGranularities()
  {

    Assert.assertTrue(
        IntervalUtils.isAligned(Intervals.ETERNITY, Granularities.ALL)
    );

    Assert.assertTrue(
        IntervalUtils.isAligned(
            Intervals.of("2000-01-01/2001-01-01"), Granularities.YEAR
        )
    );

    Assert.assertTrue(
        IntervalUtils.isAligned(
            Intervals.of("2000-01-01/2000-04-01"), Granularities.QUARTER
        )
    );


    Assert.assertTrue(
        IntervalUtils.isAligned(
            Intervals.of("2000-01-01/2000-02-01"), Granularities.MONTH
        )
    );

    // With the way WEEK granularities work, this needs to be aligned to an actual week
    Assert.assertTrue(
        IntervalUtils.isAligned(
            Intervals.of("1999-12-27/2000-01-03"), Granularities.WEEK
        )
    );

    Assert.assertTrue(
        IntervalUtils.isAligned(
            Intervals.of("2000-01-01/2000-01-02"), Granularities.DAY
        )
    );

    Assert.assertTrue(
        IntervalUtils.isAligned(
            Intervals.of("2000-01-01T00:00:00.000/2000-01-01T08:00:00.000"), Granularities.EIGHT_HOUR
        )
    );

    Assert.assertTrue(
        IntervalUtils.isAligned(
            Intervals.of("2000-01-01T00:00:00.000/2000-01-01T01:00:00.000"), Granularities.HOUR
        )
    );

    Assert.assertTrue(
        IntervalUtils.isAligned(
            Intervals.of("2000-01-01T00:00:00.000/2000-01-01T00:01:00.000"), Granularities.MINUTE
        )
    );

    Assert.assertTrue(
        IntervalUtils.isAligned(
            Intervals.of("2000-01-01T00:00:00.000/2000-01-01T00:00:01.000"), Granularities.SECOND
        )
    );

    Assert.assertFalse(
        IntervalUtils.isAligned(
            Intervals.of("2000-01-01/2002-01-01"), Granularities.YEAR
        )
    );

    Assert.assertFalse(
        IntervalUtils.isAligned(
            Intervals.of("2000-01-01/2002-01-08"), Granularities.YEAR
        )
    );
  }

  @Test
  public void test_doesIntervalMatchesGranularity_withPeriodGranularity()
  {
    Assert.assertTrue(
        IntervalUtils.isAligned(
            Intervals.of("2000-01-01/2000-01-04"),
            new PeriodGranularity(new Period("P3D"), DateTimes.of("2000-01-01"), null)
        )
    );

    Assert.assertFalse(
        IntervalUtils.isAligned(
            Intervals.of("2000-01-01/2000-01-04"),
            new PeriodGranularity(new Period("P3D"), DateTimes.of("2000-01-02"), null)
        )
    );
  }

  public static List<Interval> intervals(final String... intervals)
  {
    return Arrays.stream(intervals).map(Intervals::of).collect(Collectors.toList());
  }
}
