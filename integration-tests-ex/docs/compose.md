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

# Docker Compose Configuration

The integration tests use Docker Compose to launch Druid clusters. Each
test defines its own cluster
depending on what is to be tested. Since a large amount of the definition is
common, we use inheritance to simplify cluster definition.

Tests are split into categories so that they can run in parallel. Some of
these categories use the same cluster configuration. To further reduce
redundancy, test categories can share cluster configurations.

See also:

* [Druid configuration](druid-config.md) which is done via Compose.
* [Test configuration](test-config.md) which tells tests about the
  cluster configuration.
* [Docker compose specification](https://github.com/compose-spec/compose-spec/blob/master/spec.md)

## File Structure

Docker Compose files live in the `druid-it-cases` module (`cases` folder)
in the `cluster` directory. There is a separate subdirectory for each cluster type
(subset of test categories), plus a `Common` folder for shared files.

### Cluster Directory

Each test category uses an associated cluster. In some cases, multiple tests use
the same cluster definition. Each cluster is defined by a directory in
`$MODULE/cluster/$CLUSTER_NAME`. The directory contains a variety of files, most
of which are optional:

* `docker-compose.yaml` - Docker composes file, if created explicitly.
* `docker-compose.py` - Docker compose "template" if generated. The Python template
  format is preferred. (One of the `docker-compose.*` files is required)
* `verify.sh` - Verify the environment for the cluster. Cloud tests require that a
  number of environment variables be set to pass keys and other setup to tests.
  (Optional)
* `setup.sh` - Additional cluster setup, such as populating the "shared" directory
  with test-specific items. (Optional)

The `verify.sh` and `setup.sh` scripts are sourced into one of the "master"
scripts and can thus make use of environment variables already set:

* `BASE_MODULE_DIR` points to `integration-tests-ex/cases` where the "base" set
  of scripts and cluster definitions reside.
* `MODULE_DIR` points to the Maven module folder that contains the test.
* `CATEGORY` gives the name of the test category.
* `DRUID_INTEGRATION_TEST_GROUP` is the cluster name. Often the same as `CATEGORY`,
  but not always.

The `set -e` option is in effect so that an any errors fail the test.

## Shared Directory

Each test has a "shared" directory that is mounted into each container to hold things
like logs, security files, etc. The directory is known as `/shared` within the container,
and resides in `target/<category>`. Even if two categories share a cluster configuration,
they will have separate local versions of the shared directory. This is important to
keep log files separate for each category.

## Base Configurations

Test clusters run some number of third-party "infrastructure" containers,
and some number of Druid service containers. For the most part, each of
these services (in Compose terms) is similar from test to test. Compose
provides [an inheritance feature](
https://github.com/compose-spec/compose-spec/blob/master/spec.md#extends)
that we use to define base configurations.

* `cluster/Common/dependencies.yaml` defines external dependencis (MySQL, Kafka, ZK
  etc.)
* `cluster/Common/druid.yaml` defines typical settings for each Druid service.

Test-specific configurations extend and customize the above.

### Druid Configuration

Docker compose passes information to Docker in the form of environment variables.
The test use a variation of the environment-variable-based configuration used in
the [public Docker image](https://druid.apache.org/docs/latest/tutorials/docker.html).
That is, variables of the form `druid_my_config` are converted, by the image launch
script, into properties of the form `my.config`. These properties are then written
to a launch-specific `runtime.properties` file.

Rather than have a test version of `runtime.properties`, instead we have a set of
files that define properties as environment variables. All are located in
`cases/cluster/Common/environment-configs`:

* `common.env` - Properties common to all services. This is the test equivalent to
  the `common.runtime.properties` file.
* `<service>.env` - Properties unique to one service. This is the test equivalent to
  the `service/runtime.properties` files.

### MySQL Driver

Unit tests can use any MySQL driver, typically MySQL or MariaDB. The tests use MySQL
by default. Choose a different driver by setting the `MYSQL_DRIVER_CLASSNAME` environment
variable when running tests. The variable chooses the selected driver both in the Druid
server running in a container, and in the test "clients".

### Special Environment Variables

Druid properties can be a bit awkward and verbose in a test environment. A number of
test-specific properties help:

* `druid_standard_loadList` - Common extension load list for all tests, in the form
  of a comma-delimited list of extensions (without the brackets.) Defined in
  `common.env`.
* `druid_test_loadList` - A list of additional extensions to load for a specific test.
  Defined in the `docker-compose.yaml` file for that test category. Do not include
  quotes.

Example test-specific list:

```text
druid_test_loadList=druid-azure-extensions,my-extension
```

The launch script combines the two lists, and adds the required brackets and quotes.

## Test-Specific Cluster

Each test has a directory named `cluster/<category>`. Docker Compose uses this name
as the cluster name which appears in the Docker desktop UI. The folder contains
the `docker-compose.yaml` file that defines the test cluster.

In the simplest case, the file just lists the services to run as extensions
of the base services:

```text
services:
  zookeeper:
    extends:
      file: ../Common/dependencies.yaml
      service: zookeeper

  broker:
    extends:
      file: ../Common/compose/druid.yaml
      service: broker
...
```

## Cluster Configuration

If a test wants to run two of some service (say Coordinator), then it
can use the "standard" definition for only one of them and must fill in
the details (especially distinct port numbers) for the second.
(See `HighAvilability` for an example.)

By default, the container and internal host name is the same as the service
name. Thus, a `broker` service resides in a `broker` container known as
host `broker` on the Docker overlay network.
The service name is also usually the log file name. Thus `broker` logs
to `/target/<category>/logs/broker.log`.

An environment variable `DRUID_INSTANCE` adds a suffix to the service
name and causes the log file to be `broker-one.log` if the instance
is `one`. The service name should have the full name `broker-one`.

Druid configuration comes from the common and service-specific environment
files in `/compose/environment-config`. A test-specific service configuration
can override any of these settings using the `environment` section.
(See [Druid Configuration](druid-config.md) for details.)
For special cases, the service can define its configuration in-line and
not load the standard settings at all.

Each service can override the Java options. However, in practice, the
only options that actually change are those for memory. As a result,
the memory settings reside in `DRUID_SERVICE_JAVA_OPTS`, which you can
easily change on a service-by-service or test-by-test basis.

Debugging is enabled on port 8000 in the container. Each service that
wishes to expose debugging must map that container port to a distinct
host port.

The easiest way understand the above is to look at a few examples.

## Service Names

The Docker Compose file sets up an "overlay" network to connect the containers.
Each is known via a host name taken from the service name. Thus "zookeeper" is
the name of the ZK service and of the container that runs ZK. Use these names
in configuration within each container.

### Host Ports

Outside of the application network, containers are accessible only via the
host ports defined in the Docker Compose files. Thus, ZK is known as `localhost:2181`
to tests and other code running outside of Docker.

## Test-Specific Configuration

In addition to the Druid configuration discussed above, the framework provides
three ways to pass test-specific configuration to the tests. All of these methods
override any configuration in the `docker-compose` or cluster `env` files.

The values here are passed into the Druid server as configuration values. The
values apply to all services. (This mechanism does not allow service-specific
values.) In all three approaches, use the `druid_` environment variable form.

Precendence is in the order below with the user file lowest priority and environment
variables highest.

### User-specific `~/druid-it/<category.env` file

If you are debugging a test, you may need to provide values specific to your setup.
Examples include user names, passwords, credentials, cloud buckets, etc. Put these
in a file in your *home* directory (not Druid development directory). Create a
subdirectory `~/druid-it`, then create a separate file for each category that you
want to customize. Create entries for your information:

```text
druid_cloud_bucket=MyBucket
```

### Test-specific `OVERRIDE_ENV` file

Build scripts can pass values into Druid via a file. Set the `OVERRIDE_ENV` environment
variable with the path to the file. Each line is formatted as above. The variable can
be set on the command line:

```bash
OVERRIDE_ENV=/tmp/special.env ./cluster.sh up Category
```

It can also be set in Maven, or passed from the build environment, through Maven, to
the script.

### Environment variables

Normally the environment of the script that runs Druid is separate from the environment
passed to the container. However, the launch script will copy across any variable that
starts with `druid_`. The variable can be set on the command line:

```bash
druid_my_config=my_value ./cluster.sh up Category
```

It can also be set in Maven, or passed from the build environment, through Maven, to
the script. This is the preferred way to pass environment-specific information from
Travis into the test containers.

## Define a Test Cluster

To define a test cluster, do the following:

* Define the overlay network.
* Extend the third-party services required (at least ZK and MySQL).
* Extend each Druid service needed. Add a `depends_on` for `zookeeper` and,
  for the Coordinator and Overlord, `metadata`.
* If you need multiple instances of the same service, extend that service
  twice, and define distinct names and port numbers.
* Add any test-specific environment configuration required.

## Generating `docker-compose.yaml` Files

Each test has somewhat different needs for its test cluster. Yet, there is a
great amount of consistency across test clusters and across services. The result,
if we create files by hand, is a great amount of copy/paste redundancy, with all
the problems that copy/paste implies.

As an alternative, the framework provides a simple-minded way to generate the
`docker-compose.yaml` file using a simple Python-based template mechanism. To use
this:

* Omit the test cluster directory: `cluster/<category>`.
* Instead, create a template file: `templates/<category>.py`.
* The minimal file appears below:

```python
from template import BaseTemplate, generate

generate(__file__, BaseTemplate())
```

The above will generate a "generic" cluster: one of each kind of service, with
either a Middle Manager or Indexer depending on the `USE_INDEXER`
env var.

You customize your specific cluster by creating a test-specific template class
which overrides the various methods that build up the cluster. By using Python,
we first build the cluster as a set of Python dictionaries and arrays, then
we let [PyYAML](https://pyyaml.org/wiki/PyYAMLDocumentation) convert the objects
to a YAML file. Many methods exist to help you populate the configuration tree.
See any of the existing files for examples.

For example, you can:

* Add test-specific environment config to one, some or all services.
* Add or remove services.
* Create multiples of selected services.

The advantage is that, as Druid evolves and we change the basics, those changes
are automatically propagated to all test clusters.

Once you've created your file, the test framework will re-generate the
`docker-compose.yaml` file on each run to reflect any per-run customization.
The generated file is found in `target/cluster/<category>/docker-compose.yaml`.
As with all generated files: resist the temptation to change the generated file:
change the template instead.

The generated `docker-compose.yaml` file goes into a temporary folder:
`target/cluster/<category>`. The script copies over the `Common` directory
as well.
