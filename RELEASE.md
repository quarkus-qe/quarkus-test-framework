# Release guide

## Release streams

- main will be used for development for future RHBQ release.
- There is going to be a minor release stream for every RHBQ release - for example, 1.0.z stream for RHBQ 2.2, and 1.1.z stream for RHBQ 2.7.

## Preconditions

- The Quarkus QE Test Framework must use a released Quarkus version:

Spite the test framework does not include Quarkus upstream dependencies, it needs them to run some verifications. Also, as part of the release process we trigger this verification and we don't build Quarkus upstream, so we need to use a released Quarkus version instead of 999-SNAPSHOT in the `pom.xml`.

## Steps

1. Clone the `https://github.com/quarkus-qe/quarkus-test-framework` repository from your local machine
2. Update the `.github/project.yml` with the new version:

```yml
name: Quarkus QE Test Framework
release:
  current-version: 0.0.3
  next-version: 0.0.4-SNAPSHOT
```

3. Commit the changes and create a pull request (NOT from your fork)
4. Wait for the pull request to be green
5. Merge pull request
6. Wait for the release workflow is triggered: `https://github.com/quarkus-qe/quarkus-test-framework/actions/workflows/release.yml`
7. Edit and publish the release draft: `https://github.com/quarkus-qe/quarkus-test-framework/releases`