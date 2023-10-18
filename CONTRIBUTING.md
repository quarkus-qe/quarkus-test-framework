# Contributing guide

**Want to contribute? Great!** 
We try to make it easy, and all contributions, even the smaller ones, are more than welcome.
This includes bug reports, fixes, documentation, examples... 
But first, read this page (including the small print at the end).

* [Legal](#legal)
* [Reporting an issue](#reporting-an-issue)
* [Checking an issue is fixed in main](#checking-an-issue-is-fixed-in-main)
  + [Building main](#building-main)
* [Before you contribute](#before-you-contribute)
  + [Code reviews](#code-reviews)
  + [Coding Guidelines](#coding-guidelines)
  + [Continuous Integration](#continuous-integration)
  + [Tests and documentation are not optional](#tests-and-documentation-are-not-optional)
* [Setup](#setup)
  + [Code Style](#code-style)
* [Usage](#usage)
  + [Test Coverage](#test-coverage)
* [The small print](#the-small-print)

<small><i><a href='http://ecotrust-canada.github.io/markdown-toc/'>Table of contents generated with markdown-toc</a></i></small>

## Legal

All original contributions to Quarkus are licensed under the
[ASL - Apache License](https://www.apache.org/licenses/LICENSE-2.0),
version 2.0 or later, or, if another license is specified as governing the file or directory being
modified, such other license.

All contributions are subject to the [Developer Certificate of Origin (DCO)](https://developercertificate.org/).
The DCO text is also included verbatim in the [dco.txt](dco.txt) file in the root directory of the repository.

## Reporting an issue

This project uses GitHub issues to manage the issues. Open an issue directly in GitHub.

If you believe you found a bug, and it's likely possible, please indicate a way to reproduce it, what you are seeing and what you would expect to see.
Don't forget to indicate your Quarkus, Java, Maven/Gradle and GraalVM version. 

## Checking an issue is fixed in main

Sometimes a bug has been fixed in the `main` branch of Quarkus Test Framework and you want to confirm it is fixed for your own application.
If you are interested in having more details, refer to the [Build section](#build) and the [Usage section](#usage).

### Building main

Just do the following:

```
git clone git@github.com:quarkus-qe/quarkus-test-framework.git
cd quarkus-test-framework
mvn clean install -Pframework
```

Then you'll be able to build and run examples, as well, e.g.:
```shell
mvn clean verify -pl examples/greetings/
```

## Before you contribute

To contribute, use GitHub Pull Requests, from your **own** fork.

Also, make sure you have set up your Git authorship correctly:

```
git config --global user.name "Your Full Name"
git config --global user.email your.email@example.com
```

If you use different computers to contribute, please make sure the name is the same on all your computers.

We use this information to acknowledge your contributions in release announcements.

### Code reviews

All submissions, including submissions by project members, need to be reviewed before being merged.

### Coding Guidelines

 * We decided to disallow `@author` tags in the JavaDoc: they are hard to maintain, especially in a very active project, and we use the Git history to track authorship. GitHub also has [this nice page with your contributions](https://github.com/quarkus-qe/quarkus-test-framework/graphs/contributors). 
 * Commits should be atomic and semantic. Please properly squash your pull requests before submitting them. Fixup commits can be used temporarily during the review process but things should be squashed at the end to have meaningful commits.
 We use merge commits so the GitHub Merge button cannot do that for us. If you don't know how to do that, just ask in your pull request, we will be happy to help!

### Continuous Integration

Because we are all humans, and to ensure Quarkus Test Framework is stable for everyone, all changes must go through Quarkus Test Framework continuous integration. Quarkus Test Framework CI is based on GitHub Actions, which means that everyone has the ability to automatically execute CI in their forks as part of the process of making changes. We ask that all non-trivial changes go through this process, so that the contributor gets immediate feedback, while at the same time keeping our CI fast and healthy for everyone.

The process requires only one additional step to enable Actions on your fork (clicking the green button in the actions tab). [See the full video walkthrough](https://youtu.be/egqbx-Q-Cbg) for more details on how to do this.

### Tests and documentation are not optional

Don't forget to include tests in your pull requests. 
Also don't forget the documentation (reference documentation, JavaDoc...).

Be sure to test your pull request in:

1. Java mode
2. Native mode

## Setup

If you have not done so on this machine, you need to:
 
* Install Git and configure your GitHub access
* Install Java SDK 11+ (OpenJDK recommended)
* Install Docker - Check [the installation guide](https://docs.docker.com/install/), and [the MacOS installation guide](https://docs.docker.com/docker-for-mac/install/)

### Code Style

Quarkus Test Framework has a strictly enforced code style. Code formatting is done by the Checkstyle plugin, using the config file `checkstyle.xml`. 
By default when you run `mvn clean install` the code will be formatted automatically.
When submitting a pull request the CI build will fail if running the formatter results in any code changes, so it is
recommended that you always run a full Maven build before submitting a pull request.

## Usage

After the build was successful, the artifacts are available in your local Maven repository.

### Test Coverage

Quarkus Test Framework uses JaCoCo to generate test coverage. If you would like to generate the report run `mvn install -Pcoverage`,
then change into the `coverage-report` directory and run `mvn package`. The code coverage report will be generated in
`target/site/jacoco/`.

This currently does not work on Windows as it uses a shell script to copy all the classes and files into the code coverage
module.

If you just need a report for a single module, run `mvn install jacoco:report -Pcoverage` in that module (or with `-f ...`).

## Branches
Branches are created for important Quarkus versions, usually related to RHBQ releases and community LTS releases.

When creating new branch please ensure following items:
 - There are no pending PRs for relevant RHBQ or Quarkus stream
 - Pin external application branches, see for example https://github.com/quarkus-qe/quarkus-test-framework/pull/905
 - Pin CLI to concrete stream, see for example https://github.com/quarkus-qe/quarkus-test-framework/pull/918
 - Update GH Actions to use the right Quarkus branch, see for example https://github.com/quarkus-qe/quarkus-test-framework/pull/920

## The small print

This project is an open source project, please act responsibly, be nice, polite and enjoy!