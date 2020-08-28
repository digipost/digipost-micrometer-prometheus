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
package no.digipost.monitoring.logging;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import no.digipost.monitoring.util.Minutes;

/**
 *
 * `new LoggerThresholdMetric("ROOT, 1, "error", Duration.ofMinutes(5))`
 * Example output:
 * # HELP log_events_1min_threshold
 * # TYPE log_events_1min_threshold gauge
 * log_events_5min_threshold{level="error",logger="ROOT",} 1.0
 * <p>
 * <p>
 * Example error alert:
 * - alert: MyLoggingAlert
 * expr: &gt;
 * sum by (job,name,level,logger) (increase(logback_logger_events_total[5m]))
 * &gt;=
 * max by (job,name,level,logger) (log_events_5min_threshold)
 */
class LoggerThresholdMetric implements MeterBinder {

    private final String loggerName;
    private final double threshold;
    private final String level;
    private final Minutes minutes;

    Gauge thresholdGauge;

    LoggerThresholdMetric(String loggerName, double threshold, String level, Minutes minutes) {
        this.loggerName = loggerName;
        this.threshold = threshold;
        this.level = level;
        this.minutes = minutes;

    }

    @Override
    public void bindTo(MeterRegistry registry) {

        String metricName = "log_events_" + minutes.get() + "min_threshold";
        thresholdGauge = Gauge.builder(metricName, () -> threshold)
                .tag("logger", loggerName)
                .tag("level", level)
                .register(registry);
    }
}
