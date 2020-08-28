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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Log-events metrics for specified logback appender. Dimensions for <code>level</code>, <code>logger</code>
 * <p>
 * USAGE:
 * <pre>
 * LogbackLoggerMetrics.forRootLogger().bindTo(meterRegistry);
 * </pre>
 */
public class LogbackLoggerMetrics implements MeterBinder {

    private final LoggerContext loggerContext;
    private final String loggerName;
    private Counter traceCounter;
    private Counter debugCounter;
    private Counter infoCounter;
    private Counter warnCounter;
    private Counter errorCounter;

    private List<LoggerThresholdMetric> threshold5MinMetrics = new ArrayList<>();

    private LogbackLoggerMetrics(String loggerName) {
        this.loggerName = loggerName;
        loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    }

    public static LogbackLoggerMetrics forRootLogger() {
        return new LogbackLoggerMetrics(Logger.ROOT_LOGGER_NAME);
    }

    public static LogbackLoggerMetrics forLogger(String loggerName) {
        return new LogbackLoggerMetrics(loggerName);
    }

    /**
     * Creates a new metric with name log_events_5min_threshold and dimensions level="warn" and logger="YourLoggerName"
     * @param threshold the value of the threshold metric
     * @return this
     */
    public LogbackLoggerMetrics warnThreshold5min(double threshold) {
        threshold5MinMetrics.add(new LoggerThresholdMetric(loggerName, threshold, "warn", Duration.ofMinutes(5)));
        return this;
    }

    /**
     * Creates a new metric with name log_events_5min_threshold and dimensions level="error" and logger="YourLoggerName"
     * @param threshold the value of the threshold metric
     * @return this
     */
    public LogbackLoggerMetrics errorThreshold5min(double threshold) {
        threshold5MinMetrics.add(new LoggerThresholdMetric(loggerName, threshold, "error",Duration.ofMinutes(5)));
        return this;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        traceCounter = createCounter(registry, "trace");
        debugCounter = createCounter(registry, "debug");
        infoCounter = createCounter(registry, "info");
        warnCounter = createCounter(registry, "warn");
        errorCounter = createCounter(registry, "error");

        Logger logger = loggerContext.getLogger(loggerName);
        MetricsAppender metricsAppender = new MetricsAppender();
        metricsAppender.setContext(loggerContext);
        metricsAppender.start();

        logger.addAppender(metricsAppender);

        for (LoggerThresholdMetric loggerThresholdMetric : threshold5MinMetrics) {
            loggerThresholdMetric.bindTo(registry);
        }
    }

    private Counter createCounter(MeterRegistry registry, String level) {
        return Counter.builder("logback_logger_events")
                .tag("logger", loggerName)
                .tag("level", level)
                .register(registry);
    }

    /**
     * Based on https://github.com/prometheus/client_java/blob/master/simpleclient_logback/src/main/java/io/prometheus/client/logback/InstrumentedAppender.java
     */
    private class MetricsAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {
        @Override
        public void start() {
            super.start();
        }

        @Override
        protected void append(ILoggingEvent event) {
            try {
                switch (event.getLevel().toInt()) {
                    case Level.TRACE_INT:
                        traceCounter.increment();
                        break;
                    case Level.DEBUG_INT:
                        debugCounter.increment();
                        break;
                    case Level.INFO_INT:
                        infoCounter.increment();
                        break;
                    case Level.WARN_INT:
                        warnCounter.increment();
                        break;
                    case Level.ERROR_INT:
                        errorCounter.increment();
                        break;
                    default:
                        break;
                }
            } catch (Exception e) {
                // Guard against increment-exceptions causing more logging (however unlikely)
            }
        }

    }
}
