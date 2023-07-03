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

The framework is designed using Extension Model architecture patterns, so supporting additional features or deployment options like Kubernetes or AWS is just a matter of implementing extension points and adding dependencies into the classpath.

Main features:
- Easily deploy multiple Quarkus applications and third party components in a single scenario
- Write the test scenario once and run it everywhere (cloud, bare metal, etc)
- Developer and Test friendly
- Quarkus features focused (allow to define custom source classes, build/runtime properties, etc)
- Test isolation: for example, in OpenShift or Kubernetes, tests will be executed in an ephemeral namespace

# Requirements

- JDK 11+
- Maven 3+
- Docker
- Helm
- Helmfiles
- OCP/K8s client

# Getting Started

First, we need a Quarkus application that we want to verify. If you don't have one, follow the [Getting Started from Quarkus guide](https://quarkus.io/guides/getting-started) or simply execute:

```s
mvn io.quarkus.platform:quarkus-maven-plugin:2.3.0.Final:create \
    -DprojectGroupId=org.acme \
    -DprojectArtifactId=getting-started \
    -DclassName="org.acme.getting.started.GreetingResource" \
    -Dpath="/hello"
cd getting-started
```

The above Maven command will create a Quarkus application with a REST endpoint at `/hello`.

Then, we need to add the `quarkus-test-parent` bom dependency under the `dependencyManagement` section in the `pom.xml` file:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.quarkus.qe</groupId>
            <artifactId>quarkus-test-parent</artifactId>
            <version>${quarkus.qe.framework.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

Be sure you're using [the latest version](https://search.maven.org/search?q=a:quarkus-test-parent)!

Now, we can add the core dependency in the `pom.xml` file:

```xml
<dependency>
	<groupId>io.quarkus.qe</groupId>
	<artifactId>quarkus-test-core</artifactId>
    <scope>test</scope>
</dependency>
```

And finally, let's write our first scenario:

```java
@QuarkusScenario
public class GreetingResourceTest {

    @Test
    public void testHelloEndpoint() {
        given()
          .when().get("/hello")
          .then()
             .statusCode(200)
             .body(is("Hello RESTEasy"));
    }

}
```

Output:

```c
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running org.acme.getting.started.GreetingResourceTest
08:38:57,019 INFO  JBoss Threads version 3.4.2.Final
08:38:58,054 Quarkus augmentation completed in 1479ms
08:38:58,054 INFO  Quarkus augmentation completed in 1479ms
08:38:58,072 INFO  [app] Initialize service (Quarkus JVM mode)
08:38:58,085 INFO  Running command: java -Dquarkus.log.console.format=%d{HH:mm:ss,SSS} %s%e%n -Dquarkus.http.port=1101 -jar /home/jcarvaja/sources/tmp/getting-started/target/GreetingResourceTest/app/quarkus-app/quarkus-run.jar
08:39:01,130 INFO  [app] __  ____  __  _____   ___  __ ____  ______ 
08:39:01,134 INFO  [app]  --/ __ \/ / / / _ | / _ \/ //_/ / / / __/ 
08:39:01,135 INFO  [app]  -/ /_/ / /_/ / __ |/ , _/ ,< / /_/ /\ \   
08:39:01,136 INFO  [app] --\___\_\____/_/ |_/_/|_/_/|_|\____/___/   
08:39:01,137 INFO  [app] 08:38:58,980 Quarkus 2.3.0.Final on JVM started in 0.813s. Listening on: http://0.0.0.0:1101
08:39:01,138 INFO  [app] 08:38:58,985 Profile prod activated. 
08:39:01,139 INFO  [app] 08:38:58,986 Installed features: [cdi, resteasy, smallrye-context-propagation, vertx]
08:39:01,147 INFO  [app] Service started (Quarkus JVM mode)
08:39:01,575 INFO  ## Running test GreetingResourceTest.testHelloEndpoint()
08:39:06,804 INFO  [app] Service stopped (Quarkus JVM mode)
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 12.72 s - in org.acme.getting.started.GreetingResourceTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
```

# Documentation

Do you want to know more? Go to [our Wiki](https://github.com/quarkus-qe/quarkus-test-framework/wiki) to see all the awesome features the Quarkus Test Framework have.
