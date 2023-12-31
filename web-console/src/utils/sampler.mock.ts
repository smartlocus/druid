/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Just to make sure we are in a test context. This line will cause trouble if this file is ever compiled into the main build
import type { SampleResponse } from './sampler';

expect(1).toEqual(1); // Just to make sure this file does not get included in the build by accident

export const EMPTY_SAMPLE: SampleResponse = {
  numRowsRead: 0,
  numRowsIndexed: 0,
  logicalDimensions: [],
  physicalDimensions: [],
  logicalSegmentSchema: [{ name: '__time', type: 'LONG' }],
  data: [],
};

/*
This data is the returned sample when sampling (from the timestamp stage):

{"timestamp":"2016-04-11T09:20:00Z","user":"Alice","followers":10,"spend":0,"id":"12232323","tags":null,"nums":[4]}
{"timestamp":"2016-04-11T09:21:00Z","user":"Bob","followers":0,"spend":3,"id":"45345634","tags":["a"],"nums":[5,6]}
{"timestamp":"2016-04-11T09:22:00Z","user":"Alice","followers":3,"spend":5.1,"id":"73534533","tags":["a","b"],"nums":[7,8]}
 */

export const JSON_SAMPLE: SampleResponse = {
  numRowsRead: 3,
  numRowsIndexed: 3,
  logicalDimensions: [
    { type: 'string', name: 'user', multiValueHandling: 'SORTED_ARRAY', createBitmapIndex: true },
    {
      type: 'long',
      name: 'followers',
      multiValueHandling: 'SORTED_ARRAY',
      createBitmapIndex: false,
    },
    { type: 'json', name: 'spend', multiValueHandling: 'SORTED_ARRAY', createBitmapIndex: true },
    { type: 'string', name: 'id', multiValueHandling: 'SORTED_ARRAY', createBitmapIndex: true },
    { type: 'json', name: 'tags', multiValueHandling: 'SORTED_ARRAY', createBitmapIndex: true },
    { type: 'json', name: 'nums', multiValueHandling: 'SORTED_ARRAY', createBitmapIndex: true },
  ],
  physicalDimensions: [
    { type: 'auto', name: 'user', multiValueHandling: 'SORTED_ARRAY', createBitmapIndex: true },
    {
      type: 'auto',
      name: 'followers',
      multiValueHandling: 'SORTED_ARRAY',
      createBitmapIndex: true,
    },
    { type: 'auto', name: 'spend', multiValueHandling: 'SORTED_ARRAY', createBitmapIndex: true },
    { type: 'auto', name: 'id', multiValueHandling: 'SORTED_ARRAY', createBitmapIndex: true },
    { type: 'auto', name: 'tags', multiValueHandling: 'SORTED_ARRAY', createBitmapIndex: true },
    { type: 'auto', name: 'nums', multiValueHandling: 'SORTED_ARRAY', createBitmapIndex: true },
  ],
  logicalSegmentSchema: [
    { name: '__time', type: 'LONG' },
    { name: 'user', type: 'STRING' },
    { name: 'followers', type: 'LONG' },
    { name: 'spend', type: 'COMPLEX<json>' },
    { name: 'id', type: 'STRING' },
    { name: 'tags', type: 'ARRAY<STRING>' },
    { name: 'nums', type: 'ARRAY<LONG>' },
  ],
  data: [
    {
      input: {
        timestamp: '2016-04-11T09:20:00Z',
        user: 'Alice',
        followers: 10,
        spend: 0,
        id: '12232323',
        tags: null,
        nums: [4],
      },
      parsed: {
        __time: 1460366400000,
        user: 'Alice',
        followers: 10,
        spend: 0,
        id: '12232323',
        tags: null,
        nums: [4],
      },
    },
    {
      input: {
        timestamp: '2016-04-11T09:21:00Z',
        user: 'Bob',
        followers: 0,
        spend: 3,
        id: '45345634',
        tags: ['a'],
        nums: [5, 6],
      },
      parsed: {
        __time: 1460366460000,
        user: 'Bob',
        followers: 0,
        spend: 3,
        id: '45345634',
        tags: ['a'],
        nums: [5, 6],
      },
    },
    {
      input: {
        timestamp: '2016-04-11T09:22:00Z',
        user: 'Alice',
        followers: 3,
        spend: 5.1,
        id: '73534533',
        tags: ['a', 'b'],
        nums: [7, 8],
      },
      parsed: {
        __time: 1460366520000,
        user: 'Alice',
        followers: 3,
        spend: 5.1,
        id: '73534533',
        tags: ['a', 'b'],
        nums: [7, 8],
      },
    },
  ],
};

/*
This data is the returned sample when ingested with:

"timestamp","user","followers","spend","id","tags","nums"
"2016-04-11T09:20:00.000Z","Alice","10","0","12232323","","4"
"2016-04-11T09:21:00.000Z","Bob","0","3","45345634","a","5|6"
"2016-04-11T09:22:00.000Z","Alice","3","5.1","73534533","a|b","7|8"

This data can be derived from the JSON with this query:

SELECT
  __time as "timestamp",
  user,
  followers,
  spend,
  id,
  MV_TO_STRING(tags, '|') AS tags,
  MV_TO_STRING(nums, '|') AS nums
FROM test_data
 */

export const CSV_SAMPLE: SampleResponse = {
  numRowsRead: 3,
  numRowsIndexed: 3,
  logicalDimensions: [
    { type: 'string', name: 'user', multiValueHandling: 'SORTED_ARRAY', createBitmapIndex: true },
    {
      type: 'string',
      name: 'followers',
      multiValueHandling: 'SORTED_ARRAY',
      createBitmapIndex: true,
    },
    { type: 'string', name: 'spend', multiValueHandling: 'SORTED_ARRAY', createBitmapIndex: true },
    { type: 'string', name: 'id', multiValueHandling: 'SORTED_ARRAY', createBitmapIndex: true },
    { type: 'json', name: 'tags', multiValueHandling: 'SORTED_ARRAY', createBitmapIndex: true },
    { type: 'json', name: 'nums', multiValueHandling: 'SORTED_ARRAY', createBitmapIndex: true },
  ],
  physicalDimensions: [
    { type: 'auto', name: 'user', multiValueHandling: 'SORTED_ARRAY', createBitmapIndex: true },
    {
      type: 'auto',
      name: 'followers',
      multiValueHandling: 'SORTED_ARRAY',
      createBitmapIndex: true,
    },
    { type: 'auto', name: 'spend', multiValueHandling: 'SORTED_ARRAY', createBitmapIndex: true },
    { type: 'auto', name: 'id', multiValueHandling: 'SORTED_ARRAY', createBitmapIndex: true },
    { type: 'auto', name: 'tags', multiValueHandling: 'SORTED_ARRAY', createBitmapIndex: true },
    { type: 'auto', name: 'nums', multiValueHandling: 'SORTED_ARRAY', createBitmapIndex: true },
  ],
  logicalSegmentSchema: [
    { name: '__time', type: 'LONG' },
    { name: 'user', type: 'STRING' },
    { name: 'followers', type: 'STRING' },
    { name: 'spend', type: 'STRING' },
    { name: 'id', type: 'STRING' },
    { name: 'tags', type: 'COMPLEX<json>' },
    { name: 'nums', type: 'COMPLEX<json>' },
  ],
  data: [
    {
      input: {
        timestamp: '2016-04-11T09:20:00.000Z',
        user: 'Alice',
        followers: '10',
        spend: '0',
        id: '12232323',
        tags: null,
        nums: '4',
      },
      parsed: {
        __time: 1460366400000,
        user: 'Alice',
        followers: '10',
        spend: '0',
        id: '12232323',
        tags: null,
        nums: '4',
      },
    },
    {
      input: {
        timestamp: '2016-04-11T09:21:00.000Z',
        user: 'Bob',
        followers: '0',
        spend: '3',
        id: '45345634',
        tags: 'a',
        nums: ['5', '6'],
      },
      parsed: {
        __time: 1460366460000,
        user: 'Bob',
        followers: '0',
        spend: '3',
        id: '45345634',
        tags: 'a',
        nums: ['5', '6'],
      },
    },
    {
      input: {
        timestamp: '2016-04-11T09:22:00.000Z',
        user: 'Alice',
        followers: '3',
        spend: '5.1',
        id: '73534533',
        tags: ['a', 'b'],
        nums: ['7', '8'],
      },
      parsed: {
        __time: 1460366520000,
        user: 'Alice',
        followers: '3',
        spend: '5.1',
        id: '73534533',
        tags: ['a', 'b'],
        nums: ['7', '8'],
      },
    },
  ],
};
