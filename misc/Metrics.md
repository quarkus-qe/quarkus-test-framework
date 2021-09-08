# Metrics

Quarkus test framework uses Prometheus Pushgateway in order to expose test metrics. This feature is enabled by default. 
You can turn off by setting `-Dts.global.metrics.enabled=false`. 
All the metrics will be tagging [the test execution properties](Execution.md).

These are the basic metrics that are exposed:

- Metrics
   - `tests_total` represents the total amount of test
   - `tests_succeed` represents the total amount of succeed test
   - `tests_ignored` represents the total amount of ignored test
   - `tests_failed` represents the total amount of failures
   - `scenario_duration_seconds` represents the total duration of the scenario

- Commons labels
   - `scenario_name` represents the running scenario (class name of the test)
   - `module_name` represents the maven modules name
   - `module_status` could be `module_success` or `module_fail`
   - `execution_build_number`
   - `execution_quarkus_version`
   - `execution_service_name`
   - `execution_platform` could be `bare-metal`, `ocp` or `k8s` 

## File exporter

By default, when the metrics extension is enabled, the metrics are exported into the file `target/logs/metrics.out`. We can
configure the output file using `-Dts.global.metrics.export.file.output=/to/metrics.out`.

## Prometheus Gateway

In order to enable the Prometheus exported, you need to provide the following system properties:
- ts.global.metrics.export.prometheus.endpoint:
        Default Value: `127.0.0.1:9091` 
        Example, `myprometheus.apps.ocp47.dynamic.quarkus:9091`
