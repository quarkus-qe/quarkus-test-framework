The purpose of this app is to verify, that apps cloned via `@GitRepositoryQuarkusApplication` can access external https resources and that https://github.com/quarkus-qe/quarkus-test-framework/issues/1778 is fixed. It is used by SSLRequestIT test in this repo and its descendants.
This app intentionally doesn't use Quarkus test framework and is not included into examples folder. It should be cloned by Quarkus framework tests when they are running.
It is stored in this repo (and not a separate one) for visibility and ease of maintenance.
