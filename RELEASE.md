# Release guide

## Preconditions

- The Quarkus QE Test Framework must use a released Quarkus version:

Spite the test framework does not include Quarkus upstream dependencies, it needs them to run some verifications. Also, as part of the release process we trigger this verification and we don't build Quarkus upstream, so we need to use a released Quarkus version instead of 999-SNAPSHOT in the `pom.xml`.

## Steps

1. Clone the `https://github.com/quarkus-qe/quarkus-test-framework` repository from your local machine
2. Update the `.github/project.yml` with the new version:

```yml
name: Quarkus QE Test Framework
release:
  current-version: 0.0.2
  next-version: 0.0.3-SNAPSHOT
```

3. Update the `coverage-report/pom.xml` with the new version:

```xml
<parent>
    <groupId>io.quarkus.qe</groupId>
    <artifactId>quarkus-test-parent</artifactId>
    <version>0.0.3-SNAPSHOT</version>
</parent>
```

4. Commit these two changes and create a pull request (NOT from your fork)
5. Wait for the pull request to be green
6. Merge pull request
7. Wait for the release workflow is triggered: `https://github.com/quarkus-qe/quarkus-test-framework/actions/workflows/release.yml`
8. Edit and publish the release draft: `https://github.com/quarkus-qe/quarkus-test-framework/releases`