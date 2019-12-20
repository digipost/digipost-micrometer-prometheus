[![Maven Central](https://maven-badges.herokuapp.com/maven-central/no.digipost/digipost-micrometer-prometheus/badge.svg)](https://maven-badges.herokuapp.com/maven-central/no.digipost/digipost-micrometer-prometheus)
![](https://github.com/digipost/digipost-micrometer-prometheus/workflows/Build%20snapshot/badge.svg)
[![License](https://img.shields.io/badge/license-Apache%202-blue)](https://github.com/digipost/digipost-micrometer-prometheus/blob/master/LICENCE)

# digipost-micrometer-prometheus

## Micrometer metrics

Usage in a `MeterRegistry`:
```java
new ApplicationInfoMetrics().bindTo(this);
```

Application metric with data from `MANIFEST.MF`.

This is what is expected to exist in the manifest:

```
Build-Jdk-Spec: 12
Implementation-Title: my-application
Git-Build-Time: 2019-12-19T22:52:05+0100
Git-Build-Version: 1.2.3
Git-Commit: ffb9099
```

This will create this metric in Prometheus running java 11:
```
# HELP app_info General build and runtime information about the application. This is a static value
# TYPE app_info gauge
app_info{application="my-application",buildNumber="ffb9099",buildTime="2019-12-19T22:52:05+0100",buildVersion="1.2.3",javaBuildVersion="12",javaVersion="11",} 1.0
```

## Simple Prometheus server

The `SimplePrometheusServer` will take a log-method in its constructor to log when the server is up.

To start the server you need your instance of `PrometheusMeterRegistry` and a port. The prometheus metrics will then be on `host:port/metrics`

```java
new SimplePrometheusServer(LOG::info)
                .startMetricsServer(
                    prometheusRegistry, 9610
                );
``` 
