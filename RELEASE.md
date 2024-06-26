# Release guide

Quarkus QE test framework uses semantic versioning. Our minor releases are tied to Quarkus LTS releases, with 
micro-releases usually containing bug-fixes or non-breaking features.

## Release streams

- `main` branch is used for development for next Quarkus LTS version. As it is a development branch, no final releases 
  are to be  produced by releasing from here. To signify they're not of release quality, the versions released from main 
  branch will always have `.Beta\D+` qualifier, e.g. `1.5.0.Beta7`.
- Once the project is feature-complete for the next Quarkus LTS version, a branch should be produced and once we're 
  confident about quality of the framework, a final release can be produced. Final release version will e.g. `1.5.0`.
- Once final release of framework is produced from a branch, the subsequent releases in the stream must be backwards 
  compatible - standard semver micro-releases. If there's a necessary breaking change, it absolutely has to be signified
  in the release notes. The micro-release number should increase, e.g. `1.5.1`, `1.5.2`, ...

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