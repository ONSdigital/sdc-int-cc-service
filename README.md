[![codecov](https://codecov.io/gh/ONSdigital/sdc-int-cc-service/branch/main/graph/badge.svg?token=K4QotTmiak)](https://codecov.io/gh/ONSdigital/sdc-int-cc-service)

# Contact Centre Data Service
This repository contains the Contact Centre Data service. This microservice is a RESTful web service implemented using [Spring Boot](http://projects.spring.io/spring-boot/).
It manages contact centre data, where a Contact Centre Data object represents an expected response from the Contact Centre Data service, which provides all the data that
is required by Contact Centre in order for it to verify the contact centre's UAC code and connect them to the relevant EQ questionnaire.

## Set Up
Do the following steps to set up the code to run locally:
* Install Java 17 locally
* Make sure that you have a suitable settings.xml file in your local .m2 directory
* Clone the sdc-int-cc-service locally

## Database
There is a dependency on PostgreSQL database. When running locally a local PostgreSQL will be needed.

To setup postgres locally, see instructions at: [database readme](database/README.md).
To manage flyway development, see [flyway readme](database/flyway.md).

## Running
There are two ways of running this service:

* The first way is from the command line after moving into the same directory as the pom.xml:
    ```bash
    mvn clean install
    mvn spring-boot:run
    ```
* The second way requires that you first create a JAR file using the following mvn command (after moving into the same directory as the pom.xml):
    ```bash
    mvn clean package
    ```
This will create the JAR file in the Target directory. You can then right-click on the JAR file (in Intellij) and choose 'Run'.

## Running the junit driven tests
There are unit tests and integration tests that can be run from maven (or an IDE of your choice). Some of the integration tests
make use of [TestContainers](https://www.testcontainers.org/) which can be used for testing against a postgreSQL database, for example.
Since TestContainers relies on a docker environment, then docker should be available to the environment that the integration tests are
run.
Following normal maven conventions, the unit test classes are suffixed with **Test** and the integration test classes are suffixed with **IT**.

### Running both unit and integration tests using maven
Any of the following methods will run both sets of tests:
```sh
  mvn clean install
  mvn clean install -Dskip.integration.tests=false
  mvn clean verify
```

### Running just the unit tests using maven
Any of the following methods will run the unit tests without running the integration tests:
```sh
  mvn clean install -Dskip.integration.tests=true
  mvn clean install -DskipITs
  mvn clean test
```

### Running just the integration tests using maven
Any of the following methods will run the integration tests without running the unit tests:
```sh
  mvn clean verify -Dtest=SomePatternThatDoesntMatchAnything -DfailIfNoTests=false
  mvn failsafe:integration-test
```

### Excluding the database integration tests when running from an IDE
Configure your test run in your IDE, such that Junit5 excludes the following tag: "db".

## Deployment to kubernetes

In order to connect to PostgreSQL provided by a GCP Cloud SQL instance, then we can deploy the service in a pod with a **Cloud SQL Auth Proxy** sidecar.

A sample deployment descriptor is provided with instructions here: [deployment README](kubernetes/README.md).

## End Point

When running successfully version information can be obtained from the info endpoint:

* localhost:8171/ccsvc/info

You can also see the current database flyway migrations with:

* localhost:8171/ccsvc/flyway

Prototype only: test data in database:

* localhost:8171/ccsvc/data/case

## Docker Image Build

Is switched off by default for clean deploy. Switch on with;

* mvn dockerfile:build -Dskip.dockerfile=false

## Copyright
Copyright (C) 2021 Crown Copyright (Office for National Statistics)

