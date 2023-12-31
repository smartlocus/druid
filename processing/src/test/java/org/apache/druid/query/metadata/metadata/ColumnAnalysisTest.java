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

package org.apache.druid.query.metadata.metadata;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.druid.segment.TestHelper;
import org.apache.druid.segment.column.ColumnType;
import org.apache.druid.testing.InitializedNullHandlingTest;
import org.junit.Assert;
import org.junit.Test;

public class ColumnAnalysisTest extends InitializedNullHandlingTest
{
  private final ObjectMapper MAPPER = TestHelper.makeJsonMapper();

  private void assertSerDe(ColumnAnalysis analysis) throws Exception
  {
    Assert.assertEquals(analysis, MAPPER.readValue(MAPPER.writeValueAsString(analysis), ColumnAnalysis.class));
  }

  @Test
  public void testFoldStringColumns() throws Exception
  {
    final ColumnAnalysis analysis1 = new ColumnAnalysis(
        ColumnType.STRING,
        "STRING",
        false,
        false,
        1L,
        2,
        "aaA",
        "Zzz",
        null
    );
    final ColumnAnalysis analysis2 = new ColumnAnalysis(
        ColumnType.STRING,
        "STRING",
        true,
        false,
        3L,
        4,
        "aAA",
        "ZZz",
        null
    );

    assertSerDe(analysis1);
    assertSerDe(analysis2);

    final ColumnAnalysis expected = new ColumnAnalysis(
        ColumnType.STRING,
        "STRING",
        true,
        false,
        4L,
        4,
        "aAA",
        "Zzz",
        null
    );

    ColumnAnalysis fold1 = analysis1.fold(analysis2);
    ColumnAnalysis fold2 = analysis2.fold(analysis1);
    Assert.assertEquals(expected, fold1);
    Assert.assertEquals(expected, fold2);

    assertSerDe(fold1);
    assertSerDe(fold2);
  }

  @Test
  public void testFoldWithNull() throws Exception
  {
    final ColumnAnalysis analysis1 = new ColumnAnalysis(
        ColumnType.STRING,
        "STRING",
        false,
        false,
        1L,
        2,
        null,
        null,
        null
    );
    Assert.assertEquals(analysis1, analysis1.fold(null));
    assertSerDe(analysis1);
  }

  @Test
  public void testFoldComplexColumns() throws Exception
  {
    final ColumnAnalysis analysis1 = new ColumnAnalysis(
        ColumnType.ofComplex("hyperUnique"),
        "hyperUnique",
        false,
        false,
        0L,
        null,
        null,
        null,
        null
    );
    final ColumnAnalysis analysis2 = new ColumnAnalysis(
        ColumnType.ofComplex("hyperUnique"),
        "hyperUnique",
        false,
        false,
        0L,
        null,
        null,
        null,
        null
    );

    assertSerDe(analysis1);
    assertSerDe(analysis2);

    final ColumnAnalysis expected = new ColumnAnalysis(
        ColumnType.ofComplex("hyperUnique"),
        "hyperUnique",
        false,
        false,
        0L,
        null,
        null,
        null,
        null
    );

    ColumnAnalysis fold1 = analysis1.fold(analysis2);
    ColumnAnalysis fold2 = analysis2.fold(analysis1);
    Assert.assertEquals(expected, fold1);
    Assert.assertEquals(expected, fold2);

    assertSerDe(fold1);
    assertSerDe(fold2);
  }

  @Test
  public void testFoldDifferentTypes() throws Exception
  {
    final ColumnAnalysis analysis1 = new ColumnAnalysis(
        ColumnType.ofComplex("hyperUnique"),
        "hyperUnique",
        false,
        false,
        1L,
        1,
        null,
        null,
        null
    );
    final ColumnAnalysis analysis2 = new ColumnAnalysis(
        ColumnType.UNKNOWN_COMPLEX,
        "COMPLEX",
        false,
        false,
        2L,
        2,
        null,
        null,
        null
    );

    assertSerDe(analysis1);
    assertSerDe(analysis2);

    final ColumnAnalysis expected = ColumnAnalysis.error("cannot_merge_diff_types: [hyperUnique] and [COMPLEX]");
    final ColumnAnalysis expected2 = ColumnAnalysis.error("cannot_merge_diff_types: [COMPLEX] and [hyperUnique]");
    ColumnAnalysis fold1 = analysis1.fold(analysis2);
    ColumnAnalysis fold2 = analysis2.fold(analysis1);
    Assert.assertEquals(expected, fold1);
    Assert.assertEquals(expected2, fold2);

    assertSerDe(fold1);
    assertSerDe(fold2);
  }

  @Test
  public void testFoldDifferentTypeSignatures() throws Exception
  {
    // not sure that this should be able to happen in real life but if messages are artifically constructed
    final ColumnAnalysis analysis1 = new ColumnAnalysis(
        ColumnType.ofComplex("hyperUnique"),
        "hyperUnique",
        false,
        false,
        1L,
        1,
        null,
        null,
        null
    );
    final ColumnAnalysis analysis2 = new ColumnAnalysis(
        ColumnType.UNKNOWN_COMPLEX,
        "hyperUnique",
        false,
        false,
        2L,
        2,
        null,
        null,
        null
    );

    assertSerDe(analysis1);
    assertSerDe(analysis2);

    final ColumnAnalysis expected = ColumnAnalysis.error(
        "cannot_merge_diff_types: [COMPLEX<hyperUnique>] and [COMPLEX]"
    );
    final ColumnAnalysis expected2 = ColumnAnalysis.error(
        "cannot_merge_diff_types: [COMPLEX] and [COMPLEX<hyperUnique>]"
    );
    ColumnAnalysis fold1 = analysis1.fold(analysis2);
    ColumnAnalysis fold2 = analysis2.fold(analysis1);
    Assert.assertEquals(expected, fold1);
    Assert.assertEquals(expected2, fold2);

    assertSerDe(fold1);
    assertSerDe(fold2);
  }

  @Test
  public void testFoldSameErrors() throws Exception
  {
    final ColumnAnalysis analysis1 = ColumnAnalysis.error("foo");
    final ColumnAnalysis analysis2 = ColumnAnalysis.error("foo");

    assertSerDe(analysis1);
    assertSerDe(analysis2);

    final ColumnAnalysis expected = new ColumnAnalysis(
        ColumnType.STRING,
        "STRING",
        false,
        false,
        -1L,
        null,
        null,
        null,
        "error:foo"
    );
    ColumnAnalysis fold1 = analysis1.fold(analysis2);
    ColumnAnalysis fold2 = analysis2.fold(analysis1);
    Assert.assertEquals(expected, fold1);
    Assert.assertEquals(expected, fold2);

    assertSerDe(fold1);
    assertSerDe(fold2);
  }

  @Test
  public void testFoldErrorAndNoError() throws Exception
  {
    final ColumnAnalysis analysis1 = ColumnAnalysis.error("foo");
    final ColumnAnalysis analysis2 = new ColumnAnalysis(
        ColumnType.STRING,
        "STRING",
        false,
        false,
        2L,
        2,
        "a",
        "z",
        null
    );

    assertSerDe(analysis1);
    assertSerDe(analysis2);

    final ColumnAnalysis expected = new ColumnAnalysis(
        ColumnType.STRING,
        "STRING",
        false,
        false,
        -1L,
        null,
        null,
        null,
        "error:foo"
    );
    ColumnAnalysis fold1 = analysis1.fold(analysis2);
    ColumnAnalysis fold2 = analysis2.fold(analysis1);
    Assert.assertEquals(expected, fold1);
    Assert.assertEquals(expected, fold2);

    assertSerDe(fold1);
    assertSerDe(fold2);
  }

  @Test
  public void testFoldDifferentErrors() throws Exception
  {
    final ColumnAnalysis analysis1 = ColumnAnalysis.error("foo");
    final ColumnAnalysis analysis2 = ColumnAnalysis.error("bar");

    assertSerDe(analysis1);
    assertSerDe(analysis2);

    final ColumnAnalysis expected = new ColumnAnalysis(
        ColumnType.STRING,
        "STRING",
        false,
        false,
        -1L,
        null,
        null,
        null,
        "error:multiple_errors"
    );
    ColumnAnalysis fold1 = analysis1.fold(analysis2);
    ColumnAnalysis fold2 = analysis2.fold(analysis1);
    Assert.assertEquals(expected, fold1);
    Assert.assertEquals(expected, fold2);

    assertSerDe(fold1);
    assertSerDe(fold2);
  }
}
