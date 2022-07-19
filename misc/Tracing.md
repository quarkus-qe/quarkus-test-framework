# Tracing

The test framework will trace all your test/method invocations, so you can review how much time took to run a test
or filter by tags as `openshift`, `bare-metal`, `k8s` or errors. 

All the [Junit Tags](https://junit.org/junit5/docs/current/user-guide/#writing-tests-tagging-and-filtering) are going to 
be included as Span tags, so then later you can filter in Jaeger by your custom tags. 

All errors are going to be tagged as `error` and the error message is going to be attached to your span as an event log.

In order to push your tracing events to your Jaeger you must provide the following system properties:
- ts.global.tracing.jaeger.endpoint:
        Default Value: `http://localhost:4317` 
        Example, `https://myjaeger.apps.ocp47.dynamic.quarkus:4317`
        
All the metrics will be tagging [the test execution properties](Execution.md).

## Jaeger Installation

- On bare metal:

```
docker run --name jaeger -e COLLECTOR_OTLP_ENABLED=true -p 16686:16686 -p 4317:4317 quay.io/jaegertracing/all-in-one:1.36
```

The JAEGER API URL will be available at `http://localhost:4317`.
The JAEGER UI URL is `http://localhost:16686`.

- On OpenShift:

```
oc new-project <PROJECT WHERE YOU WANT JAEGER TO BE INSTALLED>
oc apply -f jaeger-for-tracing.yaml
```

In order to get the Jaeger routes, do `oc get routes`:

```
jaeger-api   <JAEGER API URL>          jaeger-api   <all>                 None
jaeger-ui    <JAEGER UI URL>           jaeger-ui    <all>                 None
```

## Usage

After installing Jaeger, then we can run our test suite, for example: 

```
mvn clean verify -Dts.global.build-number="475" -Dts.global.service-name="1.2.1" -Dts.global.tracing.jaeger.endpoint="<JAEGER API URL>"
```

The spanIds within the test framework must follow the next convention:

- For a test method: `<ClassName>_<methodName>`
- For a Test Scenario: `<ClassName>`
- For a service: `<ClassName>` and the service name should be attached as a tag. For example, `AlertMonitorIT` Tags: `kafka`
