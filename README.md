<p align="center">
    <a href="https://github.com/quarkus-qe/quarkus-test-framework/graphs/contributors" alt="Contributors">
        <img src="https://img.shields.io/github/contributors/quarkus-qe/quarkus-test-framework"/></a>
    <a href="https://github.com/quarkus-qe/quarkus-test-framework/pulse" alt="Activity">
        <img src="https://img.shields.io/github/commit-activity/m/quarkus-qe/quarkus-test-framework"/></a>
    <a href="https://github.com/quarkus-qe/quarkus-test-framework/actions/workflows/daily.yaml" alt="Build Status">
        <img src="https://github.com/quarkus-qe/quarkus-test-framework/actions/workflows/daily.yaml/badge.svg"></a>
    <a href="https://github.com/quarkus-qe/quarkus-test-framework" alt="Top Language">
        <img src="https://img.shields.io/github/languages/top/quarkus-qe/quarkus-test-framework"></a>
    <a href="https://github.com/quarkus-qe/quarkus-test-framework" alt="Coverage">
        <img src=".github/badges/jacoco.svg"></a>
</p>

# Quarkus QE Test Framework

The framework is designed using Extension Model architecture patterns, so supporting additional features or deployment options like Kubernetes or AWS is just a matter of implementing extension points and providing the new Maven module in the classpath.

![Components](docs/components.png)

Main features:
- Easily deploy multiple Quarkus applications and third party components in a single test
- Write the test case once and run it everywhere (cloud, bare metal, etc)
- Developer and Test friendly
- Quarkus features focused (allow to define custom source classes, build/runtime properties, etc)
- Test isolation: for example, in OpenShift or Kubernetes, tests will be executed in an ephemeral namespace 

This framework follows the Quarkus version convention, so we can selectively specify the Quarkus version via the arguments:
- `-Dquarkus.platform.version=1.13.0.Final`
- `-Dquarkus-plugin.version=1.13.0.Final`
- `-Dquarkus.platform.group-id=io.quarkus`
- `-Dquarkus.platform.artifact-id=quarkus-universe-bom`

## Getting Started

### Building the sources

The build instructions are available in the [contribution guide](CONTRIBUTING.md).

### Using the framework

In order to write Quarkus application in your tests, you first need to add the core dependency in your `pom.xml` file;

```xml
<dependency>
	<groupId>io.quarkus.qe</groupId>
	<artifactId>quarkus-test-core</artifactId>
</dependency>
```

The framework aims to think on scenarios to verify. The easiest scenario is to cope with the coverage of the current test module:

```java
@QuarkusScenario
public class PingPongResourceIT {
    @Test
    public void shouldPingPongWorks() {
        given().get("/ping").then().statusCode(HttpStatus.SC_OK).body(is("ping"));
        given().get("/pong").then().statusCode(HttpStatus.SC_OK).body(is("pong"));
    }
}
```

This mimics the usage of the `@QuarkusTest` or `@QuarkusIntegrationTest` from the Quarkus framework. Plus, it has all the benefits of using this test framework like easy logging, tracing, etc. This behaviour can be disabled by setting `ts.global.generated-service.enabled=false`.

Another more complex scenario could include a few Quarkus instances:

```java
@QuarkusScenario
public class PingPongResourceIT {

    @QuarkusApplication(classes = PingResource.class)
    static final RestService pingApp = new RestService();

    @QuarkusApplication(classes = PongResource.class)
    static final RestService pongApp = new RestService();

    // will include ping and pong resources
    @QuarkusApplication
    static final RestService pingPongApp = new RestService();

    // ...

}
```

As seen in the above example, everything is bounded to a Service object that will contain everything needed to interact with our resources.

### Services Start Up Order

By default, the services are initialized in Natural Order of presence. For example, having:

```java
class MyParent {
    @QuarkusApplication // ... or @Container
    static final RestService firstAppInParent = new RestService();
    
    @QuarkusApplication // ... or @Container
    static final RestService secondAppInParent = new RestService();

}

@QuarkusScenario
class MyScenarioIT extends MyParent {
    @QuarkusApplication // ... or @Container
    static final RestService firstAppInChild = new RestService();

    @QuarkusApplication // ... or @Container
    static final RestService secondAppInChild = new RestService();
}
```

Then, the framework will initialize the services at this order: `firstAppInParent`,  `secondAppInParent`, `firstAppInChild` and `secondAppInChild`.

We can change this order by using the `@LookupService` annotation:

```java
class MyParent {
    @LookupService
    static final RestService appInChild; // field name must match with the service name declared in MyScenarioIT.

    @QuarkusApplication // ... or @Container
    static final RestService appInParent = new RestService().withProperty("x", () -> appInChild.getHost());
}

@QuarkusScenario
class MyScenarioIT extends MyParent {
    @QuarkusApplication // ... or @Container
    static final RestService appInChild = new RestService();
}
```

| Note that field name of the `@LookupService` must match with the service name declared in MyScenarioIT.

Now, the framework will initialize the `appInChild` service first and then the `appInParent` service.

### Configuration

Test framework allows to customise the configuration for running the test case via a `test.properties` file placed under `src/test/resources` folder.
Also, a global properties file can be specified using the system property `mvn clean verify -Dts.test.resources.file.location=path/to/custom.properties`.

All the properties can be configured globally by replacing `<YOUR SERVICE NAME>` with `global`.

The current configuration options are: 

- Enable/Disable logging (enabled by default):

```
ts.<YOUR SERVICE NAME>.log.enable=true
```

Where `<YOUR SERVICE NAME>` could be either `pingApp`, `pongApp` or `pingPongApp` following the [Getting Started](#getting-started) example. If you want to use the same property for all your services, there is a special scope called `global` for such purposes:

```
ts.global.log.enable=false
```

The above configuration will disable logging for all your services. The same can be set via system properties by running `-Dts.global.log.enable=false`.

- Timeouts

Timeouts are quite important property to, as an example, control how long to wait for a service to start. The existing options to configure timeouts are:
 
```
# Startup timeout for services is 5 minutes
ts.<global or YOUR SERVICE NAME>.startup.timeout=5m
# Default startup check poll interval is every 2 seconds
ts.<global or YOUR SERVICE NAME>.startup.check-poll-interval=2s
# Install operator timeout is 10 minutes
ts.<global or YOUR SERVICE NAME>.operator.install.timeout=10m
# Install image stream timeout is 5 minutes
ts.<global or YOUR SERVICE NAME>.imagestream.install.timeout=5m
```

In order to increase the default timeout for all the services, we can use the `global` scope. For example, to increase the default startup timeout: `ts.global.startup.timeout=10m`. Also, we can configure how often the test framework will check for the startup condition using the property `ts.global.startup.check-poll-interval=3s`. Using these two properties, we are making all the services to wait up to 10 minutes to start and checking the condition every 3 seconds.

On the other hand, if we want to update the timeout for a single service because we know that this service is quite slow, the scope of the property is the service name. For example, let's imagine that the `pingApp` service from the [Getting Started](#getting-started) section is very slow, we can use `ts.pingApp.startup.timeout=20m` to wait up to 20 min for only the ping application to start. 

How can we manipulate the overall timeout in slower environments? Let's say that our environment is twice and a half slower than a good environment, then we can instruct the test framework with:

```
ts.global.factor.timeout=2.5
```

And all the timeouts will take into account this factor value. For example, if previously, the startup timeout was 10 minutes, now it will be 10 * 2.5 = 25 minutes.

In a Multi-Module Maven test suite, if we want to configure the timeouts, we can do this via system properties:
- Increase the startup timeout only: `mvn clean verify -Dts.global.startup.timeout=20m`
- Increase all the timeouts at once using the factor: `mvn clean verify -Dts.global.factor.timeout=2.5`

### Native

The `@QuarkusScenario` annotation is also compatible with Native. This means that if we run our tests using Native build:

```
mvn clean verify -Dnative
```

The tests will be executed on Native either in bare metal, OpenShift or Kubernetes.

Note that the framework will use the generated artifacts from the Maven build goal, however if you're updating a build property or using custom sources for your Quarkus application, the framework will build the Native artifact by you. This is done at the Maven failsafe execution, so failsafe needs to have the Native properties to work propertly. For doing so, we basically need to propagate the properties this way:

```xml
<profile>
    <id>native</id>
    <build>
        <plugins>
            <plugin>
                <artifactId>maven-failsafe-plugin</artifactId>
                <executions>
                    <execution>
                        <configuration>
                            <systemProperties>
                                <native.image.path>${project.build.directory}/${project.build.finalName}-runner</native.image.path>
                                <quarkus.package.type>${quarkus.package.type}</quarkus.package.type>
                                <quarkus.native.container-build>${quarkus.native.container-build}</quarkus.native.container-build>
                                <quarkus.native.native-image-xmx>${quarkus.native.native-image-xmx}</quarkus.native.native-image-xmx>
                            </systemProperties>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
    <properties>
        <quarkus.package.type>native</quarkus.package.type>
        <quarkus.native.container-build>true</quarkus.native.container-build>
        <quarkus.native.native-image-xmx>3g</quarkus.native.native-image-xmx>
    </properties>
</profile>
```

Finally, if one of your tests are incompatible on Native, you can skip them using the `@DisabledOnNative` annotation:

```java
@QuarkusScenario
@DisabledOnNative
public class OnlyOnJvmIT {
    @QuarkusApplication(ssl = true)
    static final RestService app = new RestService();

    // ...
}
```

Similarly, we can enable tests to be run only on Native build by using the `@EnabledOnNative` annotation.

### Dev mode

The test framework supports bare metal testing of DEV mode Quarkus testing. Example:

```java
@QuarkusScenario
public class DevModeGreetingResourceIT {
    @DevModeQuarkusApplication
    static DevModeQuarkusService app = new DevModeQuarkusService();
}
```

The application will start on DEV mode and will have enabled all the live coding features.

This feature includes a new `DevModeQuarkusService` service with the following functionality:

- `enableContinuousTesting` - to enable continuous testing

```java
app.enableContinuousTesting();
```

Internally, the framework will load the DEV UI and enable the continuous testing by clicking on the HTML element.

- `modifyFile` - to modify a Java source or resources file:

```java
app.modifyFile("src/main/java/io/quarkus/qe/GreetingResource.java",content -> content.replace("victor", "manuel"));
```

- `copyFile` - to copy a Java source or resources file from one source to a destination. Note that the framework will overwrite the destination file if it exists:

```java
app.copyFile("src/test/resources/jose.properties", "src/main/resources/application.properties");
```

### Quarkus CLI

The Quarkus Test Framework supports the usage of [the Quarkus CLI tool](https://quarkus.io/version/main/guides/cli-tooling):

```java
@QuarkusScenario
public class QuarkusCliClientIT {

    @Inject
    static QuarkusCliClient cliClient;

    @Test
    public void shouldVersionMatchQuarkusVersion() {
        String cliVersion = cliClient.getVersion();
        assertEquals("Client Version " + Version.getVersion(), cliVersion);
    }

    @Test
    public void shouldCreateApplicationOnJvm() {
        // Create application
        QuarkusCliRestService app = cliClient.createApplication("app");

        // Should build on Jvm
        QuarkusCliClient.Result result = app.buildOnJvm();
        assertTrue(result.isSuccessful(), "The application didn't build on JVM. Output: " + result.getOutput());

        // Start using DEV mode
        app.start();
        app.given().get().then().statusCode(HttpStatus.SC_OK);
    }
}
```

Current features:
- `run` - run any command
- `createApplication` - create a service at `target/<APP NAME>`
- `buildOnJvm` - build the service in JVM mode
- `buildOnNative` - build the service in Native mode
- `runOnDev` - run the service on DEV mode (it's the same as using `QuarkusCliRestService.start`)
- `getInstalledExtensions` - get the installed extensions
- `installExtension` - install a concrete Quarkus extension
- `removeExtension` - remove a concrete Quarkus extension

The framework will not install the Quarkus CLI tool, so before running these scenarios, it needs to be installed it beforehand.
The default command name is `quarkus`, but it can be changed using the property `ts.quarkus.cli.cmd`. For example:

```
mvn clean verify -Dts.quarkus.cli.cmd="java -jar quarkus-cli.jar"
```

The above command will use directly the binary from Quarkus upstream build.

### gRPC Integration

Internally, the test framework will map the gRPC service of our Quarkus application using a random port.
This does not work for OpenShift/Kubernetes deployments as it requires to enable HTTP/2 protocol (more information in [here](https://docs.openshift.com/container-platform/4.5/networking/ingress-operator.html#nw-http2-haproxy_configuring-ingress)).

We can enable the gRPC feature to test Quarkus application using the `@QuarkusApplication(grpc = true)` annotation. This way we can verify purely gRPC applications using the `GrpcService` service wrapper:

```java
@QuarkusScenario
public class GrpcServiceIT {

    static final String NAME = "Victor";

    @QuarkusApplication(grpc = true) // enable gRPC support
    static final GrpcService app = new GrpcService();

    @Test
    public void shouldHelloWorldServiceWork() {
        HelloRequest request = HelloRequest.newBuilder().setName(NAME).build();
        HelloReply response = GreeterGrpc.newBlockingStub(app.grpcChannel()).sayHello(request);

        assertEquals("Hello " + NAME, response.getMessage());
    }
}
```

### Disable Tests on a Concrete Quarkus version

```java
@QuarkusScenario
@DisabledOnQuarkusVersion(version = "1\\.13\\..*", reason = "https://github.com/quarkusio/quarkus/issues/XXX")
public class AuthzSecurityHttpsIT {
    
}
```

This test will not run if the quarkus version is `1.13.X`.

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
    static final DefaultService consul = new DefaultService();

    @QuarkusApplication
    static final RestService app = new RestService();

    // ...
}
```

#### Privileged Mode
Some containers require `--privileged` mode to run properly. This mode can be enabled on a per-container basis via property `ts.<YOUR SERVICE NAME>.container.privileged-mode=true` or for all containers via property `ts.global.container.privileged-mode=true`. This property only affects containers which are both: 
1) Deployed on bare metal, not in Kubernetes/OpenShift.
2) Use `@Container` annotation, not a specialised one(`@KafkaContainer`, `@AmqContainer`, etc).

#### Kafka Containers

Due to the complexity of Kafka deployments, there is a special implementation of containers for Kafka that we can use by adding the dependency:

```xml
<dependency>
    <groupId>io.quarkus.qe</groupId>
    <artifactId>quarkus-test-service-kafka</artifactId>
    <scope>test</scope>
</dependency>
```

And now, we can use the Kafka container in our test:

```java
@QuarkusScenario
public class StrimziKafkaWithoutRegistryMessagingIT {

    @KafkaContainer
    static final KafkaService kafka = new KafkaService();

    @QuarkusApplication
    static final RestService app = new RestService()
            .withProperty("kafka.bootstrap.servers", kafka::getBootstrapUrl);

    // ...
}
```

By default, the KafkaContainer will use the Strimzi implementation and Registry (with Apicurio):

```java
@QuarkusScenario
public class StrimziKafkaWithRegistryMessagingIT {

    @KafkaContainer(withRegistry = true)
    static final KafkaService kafka = new KafkaService();

    @QuarkusApplication
    static final RestService app = new RestService()
            .withProperties("strimzi-application.properties")
            .withProperty("kafka.bootstrap.servers", kafka::getBootstrapUrl)
            .withProperty("strimzi.registry.url", kafka::getRegistryUrl);

    // ...
}
```

We can also use a Confluent kafka container by doing:

```java
@KafkaContainer(vendor = KafkaVendor.CONFLUENT)
```

Note that this implemenation supports also registry, but not Kubernetes and OpenShift scenarios.

#### AMQ Containers

Similar to Kafka, we have a default implementation of an AMQ container (Artemis vendor):

```java
@QuarkusScenario
public class AmqIT {

    @AmqContainer
    static final AmqService amq = new AmqService();

    @QuarkusApplication
    static final RestService app = new RestService()
            .withProperty("quarkus.artemis.username", amq.getAmqUser())
            .withProperty("quarkus.artemis.password", amq.getAmqPassword())
            .withProperty("quarkus.artemis.url", amq::getUrl);
```

We can specify a different image by setting `@AmqContainer(image = XXX)`.
This container is compatible with OpenShift, but not with Kubernetes deployments.

#### Jaeger Containers

Required dependency

```xml
<dependency>
    <groupId>io.quarkus.qe</groupId>
    <artifactId>quarkus-test-service-jaeger</artifactId>
    <scope>test</scope>
</dependency>
```

Usage example:

```
@JaegerContainer
static final JaegerService jaeger = new JaegerService();

@QuarkusApplication
static final RestService app = new RestService().withProperty("quarkus.jaeger.endpoint", jaeger::getRestUrl);
```
This container is compatible with OpenShift, but not with Kubernetes deployments.

#### Use custom templates for Containers

Sometimes deploying a third party into OpenShift or Kubernetes involves some complex configuration that is not required when deploying it on bare metal. For these scenarios, we allow to provide a custom template via `test.properties`:

```
ts.consul.openshift.template=/yourtemplate.yaml
```

Similarly, when deploying into kubernetes, we have the property `ts.consul.kubernetes.template`.

| Note that this is only supported for OpenShift and Kubernetes. 
| And the custom template must contain ONLY ONE deployment config (for OpenShift) or ONE deployment (for kubernetes).

Moreover, if the service that is exposing the port we want to target is named differently to our service, we can provide the service name via:

```
ts.consul.openshift.service=consul-http-service
```

Same with Kubernetes `ts.consul.kubernetes.service`.

What about if we want to use the internal service as route (not the exposed route), we can set this behaviour by enabling the property `ts.<MY_SERVICE>.openshift.use-internal-service-as-url`:

```
ts.consul.openshift.use-internal-service-as-url=true
```

Same with Kubernetes `ts.consul.openshift.use-internal-service-as-url`. 

## Architecture

This framework is designed to follow **extension model** patterns. Therefore, we can extend any functionality just by adding other dependencies that extend the current functionality. As an example, Quarkus applications will be deployed locally, but if we add the OpenShift module. we can automatically deploy it in OpenShift/K8s just by adding the `@OpenShiftScenario`.

### Concepts

- `Scenario` - infrastructure to bootstrap test cases
- `Services` - how test cases interact with resources
- `Resources` - resource to manage the lifecycle of an application
- `Managed Resource` - environment where resources will be running

### Extension Points

- `Bootstrap extension point` - create your own custom scenario. Examples: OpenShift, Kubernetes, Kogito, Camel, …
- `Annotations extension point` - create your own annotations. @QuarkusApplication, @KafkaContainer, @Container, ...
- `Deployments extension point` - deploy your resources into custom environments: Localhost, OpenShift, Kubernetes

### Packages

Modules within the testing framework must conform to the following package naming conventions:

- `io.quarkus.test.bootstrap` - manage the lifecycle of the tests
- `io.quarkus.test.bootstrap.inject` - services that are injectable at test method level
- `io.quarkus.test.configuration` - configuration facilities
- `io.quarkus.test.logging` - logging facilities and handlers
- `io.quarkus.test.tracing` - tracing facilities
- `io.quarkus.test.scenarios` - scenarios that the module implement, eg: `@OpenShiftScenario`
- `io.quarkus.test.scenarios.annotations` - useful JUnit annotations to disable/enable scenarios
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

And now, we can write also scenarios to be run in OpenShift by adding the `@OpenShiftScenario`.

#### Enable/Disable Project Deletion on Failures

By default, the framework will always delete the OpenShift project and, sometimes, it's useful to not delete 
the OpenShift project on failures to troubleshooting purposes. For disabling the deletion, we need to run the 
test using:

```
mvn clean verify -Dts.openshift.delete.project.after.all=false
```

#### Operators

The OpenShift scenarios support Operator based test cases. There are two ways to deal with Operators:

- Installing the Operators as part of the `OpenShiftScenario`:

```java
@OpenShiftScenario(
        operators = @Operator(name = "strimzi-kafka-operator")
)
public class StrimziOperatorKafkaWithoutRegistryMessagingIT {
    // We can now use the new Operator CRDs manually
}
```

- Installing and managing Custom Resource Definitions as services

First, we need to create our Custom Resource YAML file, for example, for Kafka:

```yaml
apiVersion: kafka.strimzi.io/v1beta2
kind: Kafka
metadata:
  name: kafka-instance
spec:
  ...
```

Now, we can create an OperatorService to load this YAML as part of an Operator installation:

```java
@OpenShiftScenario
public class OperatorExampleIT {

    @Operator(name = "my-operator", source = "...")
    static final OperatorService operator = new OperatorService().withCrd("kafka-instance", "/my-crd.yaml");

    @QuarkusApplication
    static final RestService app = new RestService();

    // ...
}
```

The framework will install the operator and load the YAML file by you.

Note that the framework will wait for the operator to be installed before loading the CRD yaml files, but will not wait for the CRDs to be ready. If you are working with CRDs that update conditions, then we can ease this for you by providing the custom resource definition:

```java
@Version("v1beta2")
@Group("kafka.strimzi.io")
@Kind("Kafka")
public class KafkaInstanceCustomResource
        extends CustomResource<CustomResourceSpec, CustomResourceStatus>
        implements Namespaced {
}
```

And then registering the CRD with this type:

```java
@OpenShiftScenario
public class OperatorExampleIT {

    @Operator(name = "my-operator", source = "...")
    static final OperatorService operator = new OperatorService().withCrd("kafka-instance", "/my-crd.yaml", KafkaInstanceCustomResource.class);

    @QuarkusApplication
    static final RestService app = new RestService();

    // ...
}
```

Now, the framework will wait for the operator to be installed and the custom resource named `kafka-instance` to be with a condition "Ready" as "True".

#### Deployment Strategies

- **(Default) Using Build**

This strategy will build the Quarkus app artifacts locally and push it into OpenShift to generate the image that will be deployed. 

Example:

```java
@OpenShiftScenario // or @OpenShiftScenario(deployment = OpenShiftDeploymentStrategy.Build)
public class OpenShiftPingPongResourceIT {
    @QuarkusApplication(classes = PingResource.class)
    static final RestService pingApp = new RestService();

    @Test
    public void shouldPingWorks() {
        pingApp.given().get("/ping").then().statusCode(HttpStatus.SC_OK).body(is("ping"));
        pingApp.given().get("/pong").then().statusCode(HttpStatus.SC_NOT_FOUND);
    }
}
```

The default template used by this strategy can be overwritten using the property `ts.global.openshift.template`. 

- **OpenShift Extension**

This strategy will delegate the deployment into the Quarkus OpenShift extension, so it will trigger a Maven command to run it. 

Example:

```java
@OpenShiftScenario(deployment = OpenShiftDeploymentStrategy.UsingOpenShiftExtension)
public class OpenShiftPingPongResourceIT {
    // ...
}
```

In order to use this strategy, you need to add this Maven profile into the pom.xml:

```xml
<profile>
    <id>deploy-to-openshift-using-extension</id>
    <dependencies>
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-openshift</artifactId>
        </dependency>
    </dependencies>
</profile>
```

| Important note: This strategy does not support custom sources to be selected, this means that the whole Maven module will be deployed. Therefore, if we have:

```java
@OpenShiftScenario(deployment = OpenShiftDeploymentStrategy.UsingOpenShiftExtension)
public class OpenShiftUsingExtensionPingPongResourceIT {
    @QuarkusApplication(classes = PingResource.class)
    static final RestService pingPongApp = new RestService();
    
    // ...
}
```

The test case will fail saying that this is not supported using the Using OpenShift strategy.

- **OpenShift Extension and Using Docker Build**

This is an extension of the `OpenShift Extension` previous deployment strategy. The only difference is that a Docker build strategy will be used:

```java
@OpenShiftScenario(deployment = OpenShiftDeploymentStrategy.UsingOpenShiftExtensionAndDockerBuildStrategy)
public class OpenShiftUsingExtensionPingPongResourceIT {
    @QuarkusApplication(classes = PingResource.class)
    static final RestService pingPongApp = new RestService();
    
    // ...
}
```

The same limitations as in `OpenShift Extension` strategy apply here too.

- **Quarkus Source S2I**

This strategy utilises source S2I process described by the Quarkus product documentation:
```shell
oc import-image --confirm ubi8/openjdk-11 --from=registry.access.redhat.com/ubi8/openjdk-11
oc new-app ubi8/openjdk-11 <git_path> --context-dir=<context_dir> --name=<project_name>
```

The application's git repository, ref, context dir and Quarkus version are all specified in `@QuarkusApplication` annotation.

Example:

```java
@OpenShiftScenario
public class OpenShiftS2iQuickstartIT {

    @GitRepositoryQuarkusApplication(repo = "https://github.com/quarkusio/quarkus-quickstarts.git", contextDir = "getting-started")
    static final RestService app = new RestService();
    //
```

It's important to note that, by default, OpenShift will build the application's source code using the Red Hat maven repository `https://maven.repository.redhat.com/ga/`. However, some applications might require some dependencies from other remote Maven repositories. In order to allow us to add another remote Maven repository, you can use `-Dts.global.s2i.maven.remote.repository=http://host:port/repo/name`. If you only want to configure different maven repositories by service, you can do it by replacing `global` to the service name, for example: `-Dts.pingPong.s2i.maven.remote.repository=...`.

The test framework will automatically load a custom maven settings with the provided maven remote repository. But if you're using a custom template, all you need to do is to configure the `settings-mvn` config map and the Maven args as follows:

```
apiVersion: build.openshift.io/v1
kind: BuildConfig
metadata:
  name: myApp
spec:
  source:
    git:
      uri: https://github.com/repo/name.git
    type: Git
    configMaps:
    - configMap:
        name: settings-mvn
      destinationDir: "/configuration"
  strategy:
    type: Source
    sourceStrategy:
      env:
      - name: MAVEN_ARGS
        value: -s /configuration/settings.xml
      // ...
```

The default template used by this strategy can be overwritten using the property `ts.global.openshift.template`.

- **Container Registry**

This strategy will build the image locally and push it to an intermediary container registry (provided by a system property). Then, the image will be pulled from the container registry in OpenShift.

```java
@OpenShiftScenario(deployment = OpenShiftDeploymentStrategy.UsingContainerRegistry)
public class OpenShiftUsingExtensionPingPongResourceIT {
    // ...
}
```

When running these tests, the container registry must be supplied as a system property:

```
mvn clean verify -Dts.container.registry-url=quay.io/<your username>
```

These tests can be disabled if the above system property is not set using the `@DisabledIfNotContainerRegistry` annotation:

```java
@OpenShiftScenario(deployment = OpenShiftDeploymentStrategy.UsingContainerRegistry)
@DisabledIfNotContainerRegistry
public class OpenShiftUsingExtensionPingPongResourceIT {
    // ...
}
```

The default template used by this strategy can be overwritten using the property `ts.global.openshift.template`.

#### Interact with the OpenShift Client directly

We can inject the OpenShift client to interact with OpenShift. This can be useful to cope with more complex scenarios like scale up/down services.

```java
import io.quarkus.test.bootstrap.inject.OpenShiftClient;
import io.quarkus.test.scenarios.OpenShiftScenario;

@OpenShiftScenario
public class OpenShiftGreetingResourceIT extends GreetingResourceIT {
    @Test
    public void shouldInjectOpenShiftClient(OpenShiftClient client) {
        // ...
        client.scaleTo(app, 2);
    }
}
```

Another option is by injecting the client directly to the test class using the `@Inject` annotation:

```java
import io.quarkus.test.bootstrap.inject.OpenShiftClient;
import io.quarkus.test.scenarios.OpenShiftScenario;

@OpenShiftScenario
public class OpenShiftGreetingResourceIT extends GreetingResourceIT {

    @Inject
    static OpenShiftClient client;
    
    @Test
    public void shouldInjectOpenShiftClient() {
        // ...
        client.scaleTo(app, 2);
    }
}
```

| Note that the injection is only supported to static fields.

#### Enable/Disable OpenShift tests via system properties

We can selectively disable/enable OpenShift tests via system properties and using the `@EnabledIfOpenShiftScenarioPropertyIsTrue` annotation:

```java
@OpenShiftScenario
@EnabledIfOpenShiftScenarioPropertyIsTrue
public class OpenShiftUsingExtensionPingPongResourceIT {
    // ...
}
```

This test will be executed only if the system property `ts.openshift.scenario.enabled` is `true`.

### Kubernetes

Requirements:
- Kubectl CLI installed
- Be connected to a running Kubernetes instance
- Public container registry where to push/pull images

Verified Environments:
- Kind using LoadBalancer: https://kind.sigs.k8s.io/docs/user/loadbalancer/ (the framework will expose services using LoadBalancer. This is not configurable yet.)

Use this Maven dependency:

```xml
<dependency>
	<groupId>io.quarkus.qe</groupId>
	<artifactId>quarkus-test-kubernetes</artifactId>
</dependency>
```

And now, we can write also scenarios to be run in Kubernetes by adding the `@KubernetesScenario`:

```java
@KubernetesScenario
public class KubernetesPingPongResourceIT {
    @QuarkusApplication(classes = PingResource.class)
    static final RestService pingApp = new RestService();

    @Test
    public void shouldPingWorks() {
        pingApp.given().get("/ping").then().statusCode(HttpStatus.SC_OK).body(is("ping"));
        pingApp.given().get("/pong").then().statusCode(HttpStatus.SC_NOT_FOUND);
    }
}
```

The default template used by this strategy can be overwritten using the property `ts.global.kubernetes.template`.

#### Enable/Disable Namespace Deletion on Failures

By default, the framework will always delete the Kubernetes namespace and, sometimes, it's useful to not delete 
the Kubernetes namespace on failures to troubleshooting purposes. For disabling the deletion on failures, we need to run the 
test using:

```
mvn clean verify -Dts.kubernetes.delete.namespace.after.all=false
```

#### Deployment Strategies

- **(Default) Container Registry** 

Kubernetes needs a container registry where to push and pull images, so we need to provide a property like:

```
mvn clean verify -Dts.container.registry-url=quay.io/<your username>
```

The container registry must automatically exposed the containers publicly.

These tests can be disabled if the above system property is not set using the `@DisabledIfNotContainerRegistry` annotation.

#### Interact with the Kubernetes Client directly

We can inject the Kubectl client to interact with Kubernetes. This can be useful to cope with more complex scenarios like scale up/down services.

```java
import io.quarkus.test.bootstrap.inject.KubectlClient;
import io.quarkus.test.scenarios.KubernetesScenario;

@KubernetesScenario
public class KubernetesGreetingResourceIT extends GreetingResourceIT {

    @Test
    public void shouldInjectKubectlClient(KubectlClient client) {
        // ...
        client.scaleTo(app, 2);
    }
}
```

Another option is by injecting the client directly to the test class using the `@Inject` annotation:

```java
import io.quarkus.test.bootstrap.inject.KubectlClient;
import io.quarkus.test.scenarios.KubernetesScenario;

@KubernetesScenario
public class KubernetesGreetingResourceIT extends GreetingResourceIT {

    @Inject
    static KubectlClient client;
    
    // ...
}
```

| Note that the injection is only supported to static fields.

#### Enable/Disable Kubernetes tests via system properties

We can selectively disable/enable OpenShift tests via system properties and using the `@EnabledIfKubernetesScenarioPropertyIsTrue` annotation:

```java
@KubernetesScenario
@EnabledIfKubernetesScenarioPropertyIsTrue
public class KubernetesGreetingResourceIT {
    // ...
}
```

This test will be executed only if the system property `ts.kubernetes.scenario.enabled` is `true`.

## Services entities

The objective of a service is to manage internal resources:

```java
@QuarkusApplication
static final DefaultService pingApp = new DefaultService();
```

This service will host the quarkus application internally, but it will also expose the functionality to interact with it: 
- `withProperty`: Intuitive usage of properties.

We can configure our resources using configuration of other resources:

```java
@Container(image = "quay.io/bitnami/consul:1.9.3", expectedLog = "Synced node info", port = 8500)
static final DefaultService consul = new DefaultService();

@QuarkusApplication
static final DefaultService app = new DefaultService().withProperty("quarkus.consul-config.agent.host-port",
        () -> consul.getHost() + ":" + consul.getPort());
```

The resources will be initiated in order of presence of the test class. 

- Service Lifecycle

The framework allows to add actions via hooks in every stage of the service lifecycle:

```java
@Container(image = "quay.io/bitnami/consul:1.9.3", expectedLog = "Synced node info", port = 8500)
static final DefaultService consul = new DefaultService().onPostStart(GreetingResourceTest::onLoadConfigureConsul);

private static final void onLoadConfigureConsul(Service service) {
    // ...
}
```

- Services are startable and stoppable by default

Any Quarkus application and containers can be stopped to cover more complex scenarios. The test framework will restart the services by you before starting a new test case.

### Rest Services

There is a custom service implementation where we can use REST assured:

```java
@QuarkusScenario
public class PingPongResourceIT {
    @QuarkusApplication
    static final RestService pingApp = new RestService();

    @Test
    public void shouldPingWorks() {
        pingApp.given().get("/ping").then().statusCode(HttpStatus.SC_OK).body(is("ping"));
    }
}
```

### Custom Services

In the same way, we can add default services to ease and share common functionality. As part of the test framework, we added the consul service as an example.

```java
public class YourCustomService extends BaseService<ConsulService> {

    // your new methods
}
```

And use it:

```java
@QuarkusScenario
public class GreetingResourceIT {

    @Container // ... or @QuarkusApplication
    static final YourCustomService app = new YourCustomService();
    
    // your methods will be available
}
```


## More Features

- Log verifications

All the services provide the logs of the running container or Quarkus application. Example of usage:

```java
@QuarkusScenario
public class DevModeMySqlDatabaseIT {

    @DevModeQuarkusApplication
    static RestService app = new RestService();

    @Test
    public void verifyLogsToAssertDevMode() {
        app.logs().assertContains("Profile dev activated. Live Coding activated");
        // or app.getLogs() to get the full list of logs.
    }
}
```

- Discovery of build time properties to build Quarkus applications

The test framework will leverage whether an application runner can be reused or it needs to trigger a new build. Note that for this feature, tests should be written as Integration Tests. 

- External Resources

We can use properties that require external resources using the `resource::` tag. For example: `.withProperty("to.property", "resource::/file.yaml");`. This works either using containers in bare metal or OpenShift/Kubernetes.

- File logging

When running a test, the output will be copied into Console and a file placed in `target/logs/tests.out`. 

For OpenShift and Kubernetes, when some test fail, the logs of all the pods within the test namespace will be copied in `target/logs` folder as well.

- Colourify logging

The test framework will output a different colour by service. This will extremely ease the troubleshooting of the logic.

- Parallel test execution

We can verify several test modules in parallel doing:

```
mvn -T 1C clean verify
```

This Maven command would use 1 thread by CPU.

- Partial SSL support

This is only supported when running tests on bare metal:

```java
@QuarkusApplication(ssl = true)
static final RestService app = new RestService();

@Test
public void shouldSayHelloWorld() {
    app.https().given().get("/greeting").then().statusCode(HttpStatus.SC_OK).body(is("Hello World!"));
}
```

- Test Tracing

The test framework will trace all your test/method invocations, so you can review how much time took to run a test
or filter by tags as `openshift`, `bare-metal`, `k8s` or errors. 

For more information about this feature, go to [the Tracing page](/misc/Tracing.md).

- Test Metrics 

The test framework will aggregate an over all metrics and push it to prometheus. 

For more information about this feature, go to [the Metrics page](/misc/Metrics.md).
