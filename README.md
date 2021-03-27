# Quarkus QE Test Framework

The framework is designed using Extension Model architecture patterns, so supporting additional features or deployment options like Kubernetes or AWS is just a matter of implementing extension points and providing the new Maven module in the classpath.

![Components](docs/components.png)

Main features:
- Easily deploy multiple Quarkus applications and third party components in a single test
- Write the test case once and run it everywhere (cloud, bare metal, etc)
- Developer and Test friendly
- Quarkus features focused (allow to define custom source classes, build/runtime properties, etc)
- Test isolation: for example, in OpenShift or Kubernetes, tests will be executed in an ephemeral namespace 

## Getting Started

In order to write Quarkus application in your tests, you first need to add the core dependency in your `pom.xml` file;

```xml
<dependency>
	<groupId>io.quarkus.qe</groupId>
	<artifactId>quarkus-test-core</artifactId>
</dependency>
```

The framework aims to think on scenarios to verify. One scenario could include a few Quarkus instances and other container resources:

```java
@QuarkusScenario
public class PingPongResourceIT {

    @QuarkusApplication(classes = PingResource.class)
    static final Service pingApp = new Service("ping");

    @QuarkusApplication(classes = PongResource.class)
    static final Service pongApp = new Service("pong");

    // will include ping and pong resources
    @QuarkusApplication
    static final Service pingPongApp = new Service("pingpong");

    // ...

}
```

As seen in the above example, everything is bounded to a Service object that will contain everything needed to interact with our resources.

Morever, the Native version of the above example:

```java
@NativeScenario
public class NativePingPongResourceIT extends PingPongResourceIT {

}
```

### Containers 

The framework also supports to deployment of third party components provided by docker. First, we need an additional module:

```xml
<dependency>
	<groupId>io.quarkus.qe</groupId>
	<artifactId>quarkus-test-containers</artifactId>
</dependency>
```

Now, we can deploy services via docker:

```java
@QuarkusScenario
public class GreetingResourceIT {

    private static final String CUSTOM_PROPERTY = "my.property";

    @Container(image = "quay.io/bitnami/consul:1.9.3", expectedLog = "Synced node info", port = 8500)
    static final Service consul = new Service("consul");

    @QuarkusApplication
    static final Service app = new Service("app");

    // ...
}
```

## Architecture

This framework is designed to follow **extension model** patterns. Therefore, we can extend any functionality just by adding other dependencies that extend the current functionality. As an example, Quarkus applications will be deployed locally, but if we add the OpenShift module. we can automatically deploy it in OpenShift/K8s just by adding the `@OpenShiftScenario`.

### Packages

The modules within the test framework must follow the next package convention:

- `io.quarkus.test.bootstrap` - manage the lifecycle of the tests
- `io.quarkus.test.bootstrap.inject` - services that are injectable at test method level
- `io.quarkus.test.configuration` - configuration facilities
- `io.quarkus.test.logging` - logging facilities and handlers
- `io.quarkus.test.scenarios` - scenarios that the module implement, eg: `@NativeScenario`, `@OpenShiftScenario`
- `io.quarkus.test.services` - services that the module implement, eg: `@QuarkusApplication`, `@Container`
- `io.quarkus.test.services.<service-name>` - bindings to configure the `service-name` to be extended or supported
- `io.quarkus.test.utils` - more utilities

## Supported Deployment Environments

By default, the framework will run all the tests on bare metal (local machine). However, we can extend this functionality by adding other modules and annotating our tests.

### OpenShift

Requirements:
- OC CLI installed
- Be connected to an running OpenShift instance - `oc login ...`

Verified Environments:
- OCP 4.6+ 

Use this Maven dependency:

```xml
<dependency>
	<groupId>io.quarkus.qe</groupId>
	<artifactId>quarkus-test-openshift</artifactId>
</dependency>
```

And now, we can write also scenarios to be run in OpenShift by adding the `@OpenShiftScenario`:

```java
@OpenShiftScenario
public class OpenShiftPingPongResourceIT {
    @QuarkusApplication(classes = PingResource.class)
    static final Service pingApp = new Service("ping");

    @Test
    public void shouldPingWorks() {
        pingApp.restAssured().get("/ping").then().statusCode(HttpStatus.SC_OK).body(is("ping"));
        pingApp.restAssured().get("/pong").then().statusCode(HttpStatus.SC_NOT_FOUND);
    }
}
```

The nature of the test framework is that you don't need to do anything special in your tests to make them work in OpenShift, the only requirement is that you need to have installed the OC command line and have logged in an existing OpenShift instance.
 
 Also, you can extend your existing tests like in Native tests:
 
 ```java
@OpenShiftScenario
@NativeScenario
public class NativeOpenShiftPingPongResourceIT extends PingPongResourceTest {
}
```

### Kubernetes

Requirements:
- Kubectl CLI installed
- Be connected to an running Kubernetes instance
- Public container registry where to push/pull images

Verified Environments:
- Kind using LoadBalancer: https://kind.sigs.k8s.io/docs/user/loadbalancer/

Use this Maven dependency:

```xml
<dependency>
	<groupId>io.quarkus.qe</groupId>
	<artifactId>quarkus-test-openshift</artifactId>
</dependency>
```

And now, we can write also scenarios to be run in OpenShift by adding the `@OpenShiftScenario`:

```java
@KubernetesScenario
public class KubernetesPingPongResourceIT {
    @QuarkusApplication(classes = PingResource.class)
    static final Service pingApp = new Service("ping");

    @Test
    public void shouldPingWorks() {
        pingApp.restAssured().get("/ping").then().statusCode(HttpStatus.SC_OK).body(is("ping"));
        pingApp.restAssured().get("/pong").then().statusCode(HttpStatus.SC_NOT_FOUND);
    }
}
```

Kubernetes needs a container registry where to push and pull images, so we need to provide a property like:

```
mvn clean verify -Dts.container.registry-url=quay.io/<your username>
```

The container registry must automatically exposed the containers publicly.

## More Features

- `restAssured`: Rest Assured integration.

Rest Assured is already integrated in our services:

```java
@QuarkusApplication
static final Service app = new Service("app");


@Test
public void shouldPing() {
    app.restAssured().get("/ping").then().statusCode(HttpStatus.SC_OK).body(is("ping pong"));
}
```

- `withProperty`: Intuitive usage of properties.

We can configure our resources using configuration of other resources:

```java
@Container(image = "quay.io/bitnami/consul:1.9.3", expectedLog = "Synced node info", port = 8500)
static final Service consul = new Service("consul");

@QuarkusApplication
static final Service app = new Service("app").withProperty("quarkus.consul-config.agent.host-port",
        () -> consul.getHost() + ":" + consul.getPort());
```

The resources will be initiated in order of presence of the test class. 

- Service Lifecycle

The framework allows to add actions via hooks in every stage of the service lifecycle:

```java
@Container(image = "quay.io/bitnami/consul:1.9.3", expectedLog = "Synced node info", port = 8500)
static final Service consul = new Service("consul").onPostStart(GreetingResourceTest::onLoadConfigureConsul);

private static final void onLoadConfigureConsul(Service service) {
    // ...
}
```

- Services are startable and stoppable by default

Any Quarkus application and containers can be stopped to cover more complex scenarios. The test framework will restart the services by you before starting a new test case.

- Discovery of build time properties to build Quarkus applications

The test framework will leverage whether an application runner can be reused or it needs to trigger a new build. Note that for this feature, tests should be written as Integration Tests. 

- Colourify logging

The test framework will output a different colour by service. This will extremely ease the troubleshooting of the logic.

You can enable the logging by adding a `test.properties` in your module with the next property:

```
ts.<YOUR SERVICE NAME>.log.enable=true
```

## TODO
- Support properties for containers services in OpenShift/Kubernetes deployments
- Make bootable inject classes at test methods
- Support Quarkus OpenShift/Kubernete extensions strategy
- Support of Quarkus Applications from external GitHub repository
- Add example with several Microprofile services
- Add example with Keycloak
- Deploy to Maven central
