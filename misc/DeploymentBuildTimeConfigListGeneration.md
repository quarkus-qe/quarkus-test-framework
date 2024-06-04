# How to detect and generate build-time config properties for extension deployment modules

Build-time config placed in extension's deployment module (build-time fixed) are not on the class-path,
therefore they cannot be detected without additional build (with custom extension that consumes all build-time properties).
That would extend test execution runtime, for which reason we generated hardcoded list instead.

To update hardcoded list, you can use steps below.
Should the script stop working, inspect `io.quarkus.docs.generation.AllConfigGenerator` class in Quarkus docs module
to see how the detection should work.

```bash
# set this env to you test framework dir path
export TFW_BASE_DIR="sources/quarkus-test-framework"
# in Quarkus main base dir run
mvn -DquicklyDocs
cd docs
cp ~/$TFW_BASE_DIR/misc/DeploymentConfigDetector.java src/main/java/io/quarkus/docs/generation/DeploymentConfigDetector.java
mvn clean test -DskipTests -DskipITs
mvn org.codehaus.mojo:exec-maven-plugin:java -Dexec.mainClass=io.quarkus.docs.generation.DeploymentConfigDetector
cp target/deployment-build-props.txt ~/$TFW_BASE_DIR/quarkus-test-core/src/main/resources/deployment-build-props.txt
```
