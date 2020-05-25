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

## EventLogger

Sometimes you want to have metrics for some event that happens in your application. And sometimes you want som kind of
alert or warning when they occur at a given rate. This implementation is a way to achieve that in a generic way.

Your application need to implement the interface `AppBusinessEvent`. We usually do that with an enum so that we have 
easy access to the instance of the event. You can se a complete implementation of this in `AppBusinessEventLoggerTest`.

```java
EventLogger eventLogger = new AppBusinessEventLogger(meterRegistry);
eventLogger.log(MyBusinessEvents.VIOLATION_WITH_WARN);
```

This should produce a prometheus scrape output like this:
```
# HELP app_business_events_1min_warn_thresholds  
# TYPE app_business_events_1min_warn_thresholds gauge
app_business_events_1min_warn_thresholds{name="VIOLATION_WITH_WARN",} 5.0
# HELP app_business_events_total  
# TYPE app_business_events_total counter
app_business_events_total{name="VIOLATION_WITH_WARN",} 1.0
```

You can then use the gauge `app_business_events_1min_warn_thresholds` to register alerts with your system. 
This is alertmanger:

```
  - alert: MyEvents
    expr: >
      sum by (job,name) (increase(app_business_events_total[5m]))
      >=
      max by (job,name) (app_business_events_1min_warn_thresholds) * 5
    labels:
      severity: warning
    annotations:
      summary: 'High event-count for `{{ $labels.name }}`'
      description: >
        Job: `{{ $labels.job }}`, event: `{{ $labels.name }}`, has 15min count of `{{ $value | printf "%.1f" }}`
```

The nice thing here is that by doing the `sum by (job, name)` you will compare only the metrics with the same
name. For this eksample that is `VIOLATION_WITH_WARN` which is your uniqe event name in the system.
