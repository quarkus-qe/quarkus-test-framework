# Test Execution

Test suites are meant to be run as batch jobs and use different versions and build numbers, hence there are some common properties 
that we can use to uniquely identify the job execution:

- ts.global.service-name: your application service name 
        Default Value: `quarkus-test-framework`
        Example `myCryptoApp`
- ts.global.build-number: could be your Jenkins pipeline build number, in order to filter in Jaeger by this build.
        Default Value: `quarkus-plugin.version` system property value, otherwise `777-default`.
- ts.global.version-number: if your application is versioned, could be the version of your application
        Default Value: the Quarkus platform version 
        
