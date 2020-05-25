/**
 * Copyright (C) Posten Norge AS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package no.digipost.monitoring.event;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;

/**
 * EventLogger-implemetation for metrics with gauges for warns and errors
 * 
 * You need to implement the logic for the actual events your self. 
 * 
 * @see AppBusinessEvent for more information.
 * 
 * Example output:
 * # HELP app_business_events_1min_warn_thresholds  
 * # TYPE app_business_events_1min_warn_thresholds gauge
 * app_business_events_1min_warn_thresholds{name="VIOLATION_WITH_WARN_AND_ERROR",} 5.0
 * # HELP app_business_events_total  
 * # TYPE app_business_events_total counter
 * app_business_events_total{name="VIOLATION_WITH_WARN_AND_ERROR",} 1.0
 * # HELP app_business_events_1min_error_thresholds  
 * # TYPE app_business_events_1min_error_thresholds gauge
 * app_business_events_1min_error_thresholds{name="VIOLATION_WITH_WARN_AND_ERROR",} 5.0
 * 
 * Example error alert:
 *   - alert: MyErrorEvents
 *     expr: &gt;
 *       sum by (job,name) (increase(app_business_events_total[5m]))
 *       &gt;=
 *       max by (job,name) (app_business_events_1min_error_thresholds) * 5
 */
public class AppBusinessEventLogger implements EventLogger {

    static final String METRIC_APP_BUSINESS_EVENTS_TOTAL = "app_business_events_total";
    static final String METRIC_APP_BUSINESS_EVENTS_WARN_THRESHOLDS = "app_business_events_1min_warn_thresholds";
    static final String METRIC_APP_BUSINESS_EVENTS_ERROR_THRESHOLDS = "app_business_events_1min_error_thresholds";

    private final MeterRegistry meterRegistry;

    public AppBusinessEventLogger(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    Counter counter(AppBusinessEvent event) {
        return this.meterRegistry.counter(METRIC_APP_BUSINESS_EVENTS_TOTAL, Tags.of("name", event.getName()));
    }

    @Override
    public void log(AppBusinessEvent event) {
        this.counter(event).increment();
        event.getWarnThreshold().ifPresent(e -> this.meterRegistry.gauge(METRIC_APP_BUSINESS_EVENTS_WARN_THRESHOLDS, Tags.of("name", event.getName()), e.getOneMinuteThreshold()));
        event.getErrorThreshold().ifPresent(e -> this.meterRegistry.gauge(METRIC_APP_BUSINESS_EVENTS_ERROR_THRESHOLDS, Tags.of("name", event.getName()), e.getOneMinuteThreshold()));
    }

}
