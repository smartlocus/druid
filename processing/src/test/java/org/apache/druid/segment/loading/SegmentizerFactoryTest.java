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

package org.apache.druid.segment.loading;

import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.druid.jackson.DefaultObjectMapper;
import org.apache.druid.jackson.SegmentizerModule;
import org.apache.druid.segment.IndexIO;
import org.apache.druid.segment.column.ColumnConfig;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;

public class SegmentizerFactoryTest
{
  @Test
  public void testFactory() throws IOException
  {
    File factoryFile = Files.createTempFile("", "factory.json").toFile();
    FileOutputStream fos = new FileOutputStream(factoryFile);
    ObjectMapper mapper = new DefaultObjectMapper();
    mapper.registerModule(new SegmentizerModule());
    IndexIO indexIO = new IndexIO(
        mapper,
        new ColumnConfig()
        {
        }
    );
    mapper.setInjectableValues(
        new InjectableValues.Std().addValue(
            IndexIO.class,
            indexIO
        )
    );
    mapper.writeValue(fos, new MMappedQueryableSegmentizerFactory(indexIO));
    fos.close();

    SegmentizerFactory factory = mapper.readValue(factoryFile, SegmentizerFactory.class);
    Assert.assertTrue(factory instanceof MMappedQueryableSegmentizerFactory);
  }
}
