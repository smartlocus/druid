---
id: datasketches-tuple
title: "DataSketches Tuple Sketch module"
---

<!--
  ~ Licensed to the Apache Software Foundation (ASF) under one
  ~ or more contributor license agreements.  See the NOTICE file
  ~ distributed with this work for additional information
  ~ regarding copyright ownership.  The ASF licenses this file
  ~ to you under the Apache License, Version 2.0 (the
  ~ "License"); you may not use this file except in compliance
  ~ with the License.  You may obtain a copy of the License at
  ~
  ~   http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing,
  ~ software distributed under the License is distributed on an
  ~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  ~ KIND, either express or implied.  See the License for the
  ~ specific language governing permissions and limitations
  ~ under the License.
  -->


This module provides Apache Druid aggregators based on Tuple sketch from [Apache DataSketches](https://datasketches.apache.org/) library. ArrayOfDoublesSketch sketches extend the functionality of the count-distinct Theta sketches by adding arrays of double values associated with unique keys.

To use this aggregator, make sure you [include](../../configuration/extensions.md#loading-extensions) the extension in your config file:

```
druid.extensions.loadList=["druid-datasketches"]
```

For additional sketch types supported in Druid, see [DataSketches extension](datasketches-extension.md).

## Aggregator

```json
{
  "type" : "arrayOfDoublesSketch",
  "name" : <output_name>,
  "fieldName" : <metric_name>,
  "nominalEntries": <number>,
  "metricColumns" : <array of strings>,
  "numberOfValues" : <number>
 }
```

|Property|Description|Required?|
|--------|-----------|---------|
|`type`|This string should always be "arrayOfDoublesSketch"|yes|
|`name`|String representing the output column to store sketch values.|yes|
|`fieldName`|A string for the name of the input field.|yes|
|`nominalEntries`|Parameter that determines the accuracy and size of the sketch. Higher k means higher accuracy but more space to store sketches. Must be a power of 2. See the [Theta sketch accuracy](https://datasketches.apache.org/docs/Theta/ThetaErrorTable) for details. |no, defaults to 16384|
|`metricColumns`|When building sketches from raw data, an array input column that contain numeric values to associate with each distinct key. If not provided, assumes `fieldName` is an `arrayOfDoublesSketch`|no, if not provided `fieldName` is assumed to be an arrayOfDoublesSketch|
|`numberOfValues`|Number of values associated with each distinct key. |no, defaults to the length of `metricColumns` if provided and 1 otherwise|

You can use the `arrayOfDoublesSketch` aggregator to:

- Build a sketch from raw data. In this case, set `metricColumns` to an array.
- Build a sketch from an existing `ArrayOfDoubles` sketch . In this case, leave `metricColumns` unset and set the `fieldName` to an `ArrayOfDoubles` sketch with `numberOfValues` doubles. At ingestion time, you must base64 encode `ArrayOfDoubles`  sketches at ingestion time.

### Example on top of raw data

Compute a theta of unique users. For each user store the `added` and `deleted` scores. The new sketch column will be called `users_theta`.

```json
{
  "type": "arrayOfDoublesSketch",
  "name": "users_theta",
  "fieldName": "user",
  "nominalEntries": 16384,
  "metricColumns": ["added", "deleted"],
}
```

### Example ingesting a precomputed sketch column

Ingest a sketch column called `user_sketches` that has a base64 encoded value of two doubles in its array and store it in a column called `users_theta`.

```json
{
  "type": "arrayOfDoublesSketch",
  "name": "users_theta",
  "fieldName": "user_sketches",
  "nominalEntries": 16384,
  "numberOfValues": 2,
}
```

## Post aggregators

### Estimate of the number of distinct keys

Returns a distinct count estimate from a given ArrayOfDoublesSketch.

```json
{
  "type"  : "arrayOfDoublesSketchToEstimate",
  "name": <output name>,
  "field"  : <post aggregator that refers to an ArrayOfDoublesSketch (fieldAccess or another post aggregator)>
}
```

### Estimate of the number of distinct keys with error bounds

Returns a distinct count estimate and error bounds from a given ArrayOfDoublesSketch. The result will be three double values: estimate of the number of distinct keys, lower bound and upper bound. The bounds are provided at the given number of standard deviations (optional, defaults to 1). This must be an integer value of 1, 2 or 3 corresponding to approximately 68.3%, 95.4% and 99.7% confidence intervals.

```json
{
  "type"  : "arrayOfDoublesSketchToEstimateAndBounds",
  "name": <output name>,
  "field"  : <post aggregator that refers to an  ArrayOfDoublesSketch (fieldAccess or another post aggregator)>,
  "numStdDevs", <number from 1 to 3>
}
```

### Number of retained entries

Returns the number of retained entries from a given ArrayOfDoublesSketch.

```json
{
  "type"  : "arrayOfDoublesSketchToNumEntries",
  "name": <output name>,
  "field"  : <post aggregator that refers to an ArrayOfDoublesSketch (fieldAccess or another post aggregator)>
}
```

### Mean values for each column

Returns a list of mean values from a given ArrayOfDoublesSketch. The result will be N double values, where N is the number of double values kept in the sketch per key.

```json
{
  "type"  : "arrayOfDoublesSketchToMeans",
  "name": <output name>,
  "field"  : <post aggregator that refers to a DoublesSketch (fieldAccess or another post aggregator)>
}
```

### Variance values for each column

Returns a list of variance values from a given ArrayOfDoublesSketch. The result will be N double values, where N is the number of double values kept in the sketch per key.

```json
{
  "type"  : "arrayOfDoublesSketchToVariances",
  "name": <output name>,
  "field"  : <post aggregator that refers to a DoublesSketch (fieldAccess or another post aggregator)>
}
```

### Quantiles sketch from a column

Returns a quantiles DoublesSketch constructed from a given column of values from a given ArrayOfDoublesSketch using optional parameter k that determines the accuracy and size of the quantiles sketch. See [Quantiles Sketch Module](datasketches-quantiles.md)

* The column number is 1-based and is optional (the default is 1).
* The parameter k is optional (the default is defined in the sketch library).
* The result is a quantiles sketch.

```json
{
  "type"  : "arrayOfDoublesSketchToQuantilesSketch",
  "name": <output name>,
  "field"  : <post aggregator that refers to a DoublesSketch (fieldAccess or another post aggregator)>,
  "column" : <number>,
  "k" : <parameter that determines the accuracy and size of the quantiles sketch>
}
```

### Set operations

Returns a result of a specified set operation on the given array of sketches. Supported operations are: union, intersection and set difference (UNION, INTERSECT, NOT).

```json
{
  "type"  : "arrayOfDoublesSketchSetOp",
  "name": <output name>,
  "operation": <"UNION"|"INTERSECT"|"NOT">,
  "fields"  : <array of post aggregators to access sketch aggregators or post aggregators to allow arbitrary combination of set operations>,
  "nominalEntries" : <parameter that determines the accuracy and size of the sketch>,
  "numberOfValues" : <number of values associated with each distinct key>
}
```

### Student's t-test

Performs Student's t-test and returns a list of p-values given two instances of ArrayOfDoublesSketch. The result will be N double values, where N is the number of double values kept in the sketch per key. See [t-test documentation](https://commons.apache.org/proper/commons-math/javadocs/api-3.6.1/org/apache/commons/math3/stat/inference/TTest.html).

```json
{
  "type"  : "arrayOfDoublesSketchTTest",
  "name": <output name>,
  "fields"  : <array with two post aggregators to access sketch aggregators or post aggregators referring to an ArrayOfDoublesSketch>,
}
```

### Sketch summary

Returns a human-readable summary of a given ArrayOfDoublesSketch. This is a string returned by toString() method of the sketch. This can be useful for debugging.

```json
{
  "type"  : "arrayOfDoublesSketchToString",
  "name": <output name>,
  "field"  : <post aggregator that refers to an ArrayOfDoublesSketch (fieldAccess or another post aggregator)>
}
```


### Constant ArrayOfDoublesSketch 

This post aggregator adds a Base64-encoded constant ArrayOfDoublesSketch value that you can use in other post aggregators.
```json
{
  "type": "arrayOfDoublesSketchConstant",
  "name": DESTINATION_COLUMN_NAME,
  "value": CONSTANT_SKETCH_VALUE
}
```

### Base64 output of ArrayOfDoublesSketch 

This post aggregator outputs an ArrayOfDoublesSketch as a Base64-encoded string storing the constant tuple sketch value that you can use in other post aggregators. 

```json
{
  "type": "arrayOfDoublesSketchToBase64String",
  "name": DESTINATION_COLUMN_NAME,
  "field": <post aggregator that refers to a ArrayOfDoublesSketch (fieldAccess or another post aggregator)>
}
```

### Estimated metrics values for each column of ArrayOfDoublesSketch

For the key-value pairs in the given ArrayOfDoublesSketch, this post aggregator estimates the sum for each set of values across the keys. For example, the post aggregator returns `{3.0, 8.0}` for the following key-value pairs:

```
Key_1, {1.0, 3.0}
Key_2, {2.0, 5.0}
```

The post aggregator returns _N_ double values, where _N_ is the number of values associated with each key.

```json
{
  "type": "arrayOfDoublesSketchToMetricsSumEstimate",
  "name": DESTINATION_COLUMN_NAME,
  "field": <post aggregator that refers to a ArrayOfDoublesSketch (fieldAccess or another post aggregator)>
}
```
