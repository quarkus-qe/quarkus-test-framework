# Quarkus test preparer

Prepares source directory for applications build in quarkus testing.
In particular it prepares custom `pom.xml` based on template from this module.

## Packaging
By default, test preparer creates apps with packaging `quarkus`.
It will ignore `packaging` attribute set in the tested project. 
To override this behaviour in your project, set property `ts.packaging` to the packaging you want.
