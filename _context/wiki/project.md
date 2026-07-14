# Project Overview

## What it is

The **Quarkus QE Test Framework** is a testing framework built by the Quarkus QE team that allows developers to:

- Easily build and deploy Quarkus applications in test scenarios
- Run tests on multiple targets — **bare metal** (JVM and native mode) and **OpenShift / Kubernetes** — from a single test definition
- Deploy multiple Quarkus applications and third-party services in a single scenario
- Isolate tests via ephemeral namespaces (on OpenShift/Kubernetes)

It is designed using an **Extension Model architecture**, meaning new features or deployment targets (e.g. Kubernetes, AWS) are added by implementing extension points and dropping dependencies on the classpath.

## Main goals and objectives

| Priority | Goal |
|---|---|
| **Active** | Improve reliability of the framework itself |
| Ongoing | Maintain compatibility with Quarkus LTS releases |
| Ongoing | Keep the framework developer- and test-friendly |

## Key stakeholders / users

- **Quarkus QE team** — primary users and maintainers

## Architecture

- Extension Model pattern — features are pluggable via classpath dependencies
- Core module: `quarkus-test-core`
- Platform modules: `quarkus-test-openshift`, `quarkus-test-kubernetes`
- Service modules (third-party integrations): `quarkus-test-service-kafka`, `quarkus-test-service-database`, `quarkus-test-service-keycloak`, and others
- Container support: `quarkus-test-containers`
- Supporting: `quarkus-test-cli`, `quarkus-test-helm`, `quarkus-test-images`, `quarkus-test-knative-events`

## Requirements

- JDK 17+
- Maven 3+
- Docker
- OCP/K8s client (for cloud targets)

## Release workflow

Versioning follows **semantic versioning** tied to Quarkus LTS releases.

| Branch | Purpose | Version format |
|---|---|---|
| `main` | Development for next LTS | `1.x.0.BetaN` |
| release branch | Stable stream for an LTS | `1.x.0`, `1.x.1`, … |

### Framework ↔ Quarkus version correspondence

Each framework branch tracks a single Quarkus LTS strean. Quarkus LTS releases are usually cut every six minor versions (2.2, 2.7, 2.13, 3.2, 3.8, 3.15, 3.20, 3.27, 3.33 …).

| Framework branch | Latest release | Quarkus LTS stream | Current Quarkus version | Status |
|---|---|---|---|---|
| `1.0.z` | `1.0.5.Final` | 2.2.x LTS | `2.2.5.Final` | EOL |
| `1.1.z` | `1.1.5.Final` | 2.7.x LTS | `2.7.7.Final` | EOL |
| `1.2.z` | `1.2.6.Final` | 2.13.x LTS | `2.13.9.Final` | EOL |
| `1.3.z` | `1.3.2.Final` | 3.2.x LTS | `3.2.12.Final` | EOL |
| `1.4.z` | `1.4.12` | 3.8.x LTS | `3.8.6` | EOL |
| `1.5.z` | `1.5.14` | 3.15.x LTS | `3.15.7` | EOL |
| `1.6.z` | `1.6.11` | 3.20.x LTS | `3.20.6` | EOL |
| `1.7.z` | `1.7.10` | 3.27.x LTS | `3.27.4` | Active |
| `1.8.z` | `1.8.5` | 3.33.x LTS | `3.33.2` | Active |
| `main` | — | next LTS | `3.999-SNAPSHOT` | Dev |

### Release steps

1. Clone the repository locally (not from a fork)
2. Update `.github/project.yml` with `current-version` and `next-version`
3. Commit, open a PR from the main repo (not a fork), wait for green CI
4. Merge the PR
5. Wait for the `release.yml` GitHub Actions workflow to complete
6. Edit and publish the release draft on GitHub Releases

> **Precondition:** `pom.xml` must reference a released Quarkus version (not `999-SNAPSHOT`) before cutting a release.

## Key links

- GitHub: https://github.com/quarkus-qe/quarkus-test-framework
- Wiki: https://github.com/quarkus-qe/quarkus-test-framework/wiki
- CI (daily): https://github.com/quarkus-qe/quarkus-test-framework/actions/workflows/daily.yaml
- Releases: https://github.com/quarkus-qe/quarkus-test-framework/releases
