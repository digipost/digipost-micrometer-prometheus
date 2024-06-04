/*
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

import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class AppBusinessEventLoggerTest {

    private PrometheusMeterRegistry prometheusRegistry;
    private EventLogger eventLogger;

    @BeforeEach
    void setUp() {
        this.prometheusRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        this.eventLogger = new AppBusinessEventLogger(prometheusRegistry);
    }

    @Test
    void should_register_event() {
        eventLogger.log(MyBusinessEvents.VIOLATION);
        assertThat(prometheusRegistry.scrape(), containsString("app_business_events_total{name=\"VIOLATION\"} 1.0"));
        assertThat(prometheusRegistry.scrape(), not(containsString("app_business_events_1min_warn_thresholds")));
        assertThat(prometheusRegistry.scrape(), not(containsString("app_business_events_1min_error_thresholds")));
    }

    @Test
    void should_register_event_with_warn() {
        eventLogger.log(MyBusinessEvents.VIOLATION_WITH_WARN);
        assertThat(prometheusRegistry.scrape(), containsString("app_business_events_total{name=\"VIOLATION_WITH_WARN\"} 1.0"));
        assertThat(prometheusRegistry.scrape(), containsString("app_business_events_1min_warn_thresholds{name=\"VIOLATION_WITH_WARN\"} 5.0"));
        assertThat(prometheusRegistry.scrape(), not(containsString("app_business_events_1min_error_thresholds")));
    }

    @Test
    void should_register_event_with_error() {
        eventLogger.log(MyBusinessEvents.VIOLATION_WITH_ERROR);
        assertThat(prometheusRegistry.scrape(), containsString("app_business_events_total{name=\"VIOLATION_WITH_ERROR\"} 1.0"));
        assertThat(prometheusRegistry.scrape(), not(containsString("app_business_events_1min_warn_thresholds")));
        assertThat(prometheusRegistry.scrape(), containsString("app_business_events_1min_error_thresholds{name=\"VIOLATION_WITH_ERROR\"} 5.0"));
    }

    @Test
    void should_register_event_with_warn_and_error() {
        eventLogger.log(MyBusinessEvents.VIOLATION_WITH_WARN_AND_ERROR);
        assertThat(prometheusRegistry.scrape(), containsString("app_business_events_total{name=\"VIOLATION_WITH_WARN_AND_ERROR\"} 1.0"));
        assertThat(prometheusRegistry.scrape(), containsString("app_business_events_1min_warn_thresholds{name=\"VIOLATION_WITH_WARN_AND_ERROR\"} 5.0"));
        assertThat(prometheusRegistry.scrape(), containsString("app_business_events_1min_error_thresholds{name=\"VIOLATION_WITH_WARN_AND_ERROR\"} 5.0"));
    }

    @Test
    void check_that_values_are_not_GCed() {
        eventLogger.log(MyBusinessEvents.VIOLATION_WITH_WARN_AND_ERROR, 1337);
        Runtime.getRuntime().gc();

        assertThat(prometheusRegistry.scrape(), not(containsString("NaN")));
    }

    private enum MyBusinessEvents implements AppBusinessEvent {
        VIOLATION,
        VIOLATION_WITH_WARN(OneMinuteEventsThreshold.perDay(7200)),
        VIOLATION_WITH_ERROR(null, OneMinuteEventsThreshold.perMinute(5)),
        VIOLATION_WITH_WARN_AND_ERROR(OneMinuteEventsThreshold.perDay(7200), OneMinuteEventsThreshold.perMinute(5));

        private final EventsThreshold warnThreshold;
        private final EventsThreshold errorThreshold;

        MyBusinessEvents() {
            this(null, null);
        }

        MyBusinessEvents(EventsThreshold warnThreshold) {
            this(warnThreshold, null);
        }

        MyBusinessEvents(EventsThreshold warnThreshold, EventsThreshold errorThreshold) {
            this.warnThreshold = warnThreshold;
            this.errorThreshold = errorThreshold;
        }

        @Override
        public String getName() {
            return name();
        }

        @Override
        public Optional<EventsThreshold> getWarnThreshold() {
            return Optional.ofNullable(this.warnThreshold);
        }

        @Override
        public Optional<EventsThreshold> getErrorThreshold() {
            return Optional.ofNullable(this.errorThreshold);
        }
    }
}
