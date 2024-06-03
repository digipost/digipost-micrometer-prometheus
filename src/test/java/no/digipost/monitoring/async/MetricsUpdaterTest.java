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
package no.digipost.monitoring.async;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import no.digipost.time.ControllableClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MetricsUpdaterTest {

    public static final String SCRAPE_ERRORS = "scrape_errors";
    public static final Runnable DO_NOTHING = () -> {};
    private final Instant now = ZonedDateTime.of(2019, 2, 2, 10, 20, 0, 0, ZoneId.systemDefault()).toInstant();
    private ControllableClock clock = ControllableClock.freezedAt(now);
    private MetricsUpdater metricsUpdater;

    @BeforeEach
    void setUp() {
        metricsUpdater = new MetricsUpdater(new SimpleMeterRegistry(), Mockito.mock(ScheduledExecutorService.class), SCRAPE_ERRORS, clock);
    }

    @Test
    void should_report_scrape_error_for_updaters_not_being_called() {
        metricsUpdater.registerAsyncUpdate("test-update", Duration.ofMinutes(1), DO_NOTHING);
        assertEquals(metricsUpdater.getScrapeErrors(), 0.0);

        clock.timePasses(Duration.ofMinutes(5));
        assertEquals(metricsUpdater.getScrapeErrors(), 1.0);

        metricsUpdater.updaters.forEach(AsyncUpdater::run);
        assertEquals(metricsUpdater.getScrapeErrors(), 0.0);
    }

    @Test
    void should_report_scrape_error_when_update_throws() {
        metricsUpdater.registerAsyncUpdate("test-update", Duration.ofMinutes(1), () -> {
            throw new RuntimeException();
        });
        assertEquals(metricsUpdater.getScrapeErrors(), 0.0);

        metricsUpdater.updaters.forEach(AsyncUpdater::run);
        assertEquals(metricsUpdater.getScrapeErrors(), 1.0);
    }

}
