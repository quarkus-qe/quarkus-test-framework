# Metrics

Quarkus test framework uses Prometheus Pushgateway in order to expose test metrics. 
This decision was made because test cases behave as batch jobs. 

Test Suite -> Prometheus Pushgateway -> prometheus -> Grafana

These are the basic metrics that are exposed:

- Metrics
   - `ts_quarkus_gauge_requests_total` represent the total amount of test
   - `ts_quarkus_gauge_requests_success` represent the total amount of succeed test
   - `ts_quarkus_gauge_requests_ignore` represent the total amount of ignored test
   - `ts_quarkus_gauge_requests_fail` represent the total amount of failures 
   - `ts_quarkus_gauge_modules` represents metrics associated to the test modules. You will have one of this metrics per quarkus scenario / module.
        - `ts_quarkus_module_name` represents the maven modules name
        - `ts_quarkus_module_status` could be `module_success` or `module_fail`
   - `ts_quarkus_histogram_modules_duration`

- Commons labels
   - `ts_quarkus_build_number`
   - `ts_quarkus_plugin_version`
   - `ts_quarkus_service_name`
   - `ts_quarkus_platform` could be `bare-metal`, `ocp` or `k8s` 
   
In order to push your metrics to prometheus you must provide the following system properties:
- ts.prometheus-http-endpoint (required):
        Default Value: `127.0.0.1:9091` 
        Example, `myprometheus.apps.ocp47.dynamic.quarkus:9091`
- ts.service-name (required): your application service name 
        Default Value: `quarkus-test-framework`
        Example `myCryptoApp`
- ts.buildNumber: could be your Jenkins pipeline build number, in order to filter in Jaeger by this build.
        Default Value: `quarkus-plugin.version` system property value, otherwise `777-default`.
- ts.versionNumber: if your application is versioned, could be the version of your application
        Default Value: `999-default`   