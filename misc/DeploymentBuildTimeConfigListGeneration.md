# How to detect and generate build-time config properties for extension deployment modules

Build-time config placed in extension's deployment module (build-time fixed) are not on the class-path,
therefore they cannot be detected without additional build (with custom extension that consumes all build-time properties).
That would extend test execution runtime, for which reason we generated hardcoded list instead.

To update hardcoded list, you can use steps below.

```bash
# set this env to you test framework dir path
export TFW_BASE_DIR="$(pwd)"
# in Quarkus main base dir run
mvn clean install -Dquickly -Dno-test-modules -Prelocations
cd docs
cp $TFW_BASE_DIR/misc/ExtractBuildTimeProperties.java src/main/java/io/quarkus/docs/generation/ExtractBuildTimeProperties.java
mvn compile exec:java -Dexec.mainClass="io.quarkus.docs.generation.ExtractBuildTimeProperties"
cp deployment-build-props.txt $TFW_BASE_DIR/quarkus-test-core/src/main/resources/deployment-build-props.txt
```
