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

## TimedThirdPartyCall

With `TimedThirdPartyCall` you can wrap your code to get metrics on the call with extended funtionality on top of what 
micrometer Timed gives you.

An example:
```java
final BiFunction<MyResponse, Optional<RuntimeException>, AppStatus> warnOnSituation = (response, possibleException) -> possibleException.isPresent() || "ERROR_SITUATION".equals(response.data) ? AppStatus.WARN : AppStatus.OK;

final TimedThirdPartyCall<MyResponse> getStuff = TimedThirdPartyCallDescriptor.create("ExternalService", "getStuff", prometheusRegistry)
        .callResponseStatus(warnOnSituation);

getStuff.call(() -> new MyResponse("ERROR_SITUATION"));
``` 

This will produce a number of metrics:
```
app_third_party_call_total{name="ExternalService_getStuff", status="OK"} 0.0
app_third_party_call_total{name="ExternalService_getStuff", status="WARN"} 1.0
app_third_party_call_total{name="ExternalService_getStuff", status="FAILED"} 0.0
app_third_party_call_seconds_count{name="ExternalService_getStuff",} 1.0
app_third_party_call_seconds_sum{name="ExternalService_getStuff",} 6.6018E-5
app_third_party_call_seconds_max{name="ExternalService_getStuff",} 6.6018E-5
```

The idea is that Timed only count exections overall. What we want in addition is finer granularity to create better alerts
in our alerting rig. By specifying a function by witch we say OK/WARN/FAILED we can exclude error-situations 
that we want to igore from alerts reacting to `FAILED` or a percentage of `FAILED/TOTAL`.

You can also use simple exception-mapper-function for a boolean OK/FAILED:
```java
TimedThirdPartyCall<String> getStuff = TimedThirdPartyCallDescriptor.create("ExternalService", "getStuff", prometheusRegistry)
    .exceptionAsFailure();

String result = getStuff.call(() -> "OK");
```

You can also defined percentiles (default 0.5, 0.95, 0.99):
```java
TimedThirdPartyCallDescriptor.create("ExternalService", "getStuff", prometheusRegistry)
        .callResponseStatus(warnOnSituation, 0.95, 0.99);
```
