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
package no.digipost.monitoring.async;

import io.micrometer.core.instrument.MeterRegistry;
import no.digipost.DiggConcurrent;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Add alert for scrape errors to Prometheus:
 *
 * <pre>{@code
 *   - alert: AsyncUpdateScrapeErrors
 *     expr: app_async_update_scrape_errors > 0
 *     for: 2m
 *     labels:
 *       severity: warning
 *     annotations:
 *       summary: Updater for one or more metrics failing
 *       description: "Job: `{{ $labels.job }}`, Instance: `{{ $labels.instance}}` reports `{{ $value }}` scrape errors. One or more updaters are failing, which means some of the metric-values may become stale. See log for details."
 * }</pre>
 */
public class MetricsUpdater {

    private static final Logger LOG = Logger.getLogger(MetricsUpdater.class.getName());
    final List<AsyncUpdater> updaters = new ArrayList<>();
    private ScheduledExecutorService scheduledExecutor;
    private Clock clock;

    public MetricsUpdater(MeterRegistry registry, int maxThreads) {
        this(registry, maxThreads, "app_async_update_scrape_errors");
    }

    public MetricsUpdater(MeterRegistry registry, int maxThreads, String scrapeErrorsMetricName) {
        this(registry,
                Executors.newScheduledThreadPool(maxThreads, DiggConcurrent.threadNamingFactory("micrometer-metrics-updater")),
                scrapeErrorsMetricName,
                Clock.systemDefaultZone());
    }

    MetricsUpdater(MeterRegistry registry, ScheduledExecutorService scheduledExecutor, String scrapeErrorsMetricName, Clock clock) {
        this.clock = clock;
        this.scheduledExecutor = scheduledExecutor;

        registry.gauge(scrapeErrorsMetricName, updaters, MetricsUpdater::getScrapeErrors);
    }

    private static double getScrapeErrors(List<AsyncUpdater> updaters) {
        AtomicInteger errors = new AtomicInteger(0);
        StringBuilder warnings = new StringBuilder();
        updaters.forEach(u -> {
            if (u.isStale()) {
                errors.incrementAndGet();
                warnings.append("AsyncUpdater '" + u.getName() + "' has not run since " + u.getLastUpdate()).append("\n");
            }
            if (u.isFailing()) {
                errors.incrementAndGet();
                warnings.append("AsyncUpdater '" + u.getName() + "' did not run successfully last time.").append("\n");
            }
        });
        if (errors.get() > 0) {
            LOG.warning(warnings.toString());
        }
        return errors.get();
    }

    public void stop() {
        DiggConcurrent.ensureShutdown(scheduledExecutor, Duration.ofSeconds(30));
    }

    public void registerAsyncUpdate(String updaterName, Duration updateInterval, Runnable setNewValues) {
        AsyncUpdater asyncUpdater = new AsyncUpdater(clock, updaterName, setNewValues, updateInterval);
        updaters.add(asyncUpdater);
        scheduledExecutor.scheduleAtFixedRate(asyncUpdater, 0, updateInterval.toMillis(), TimeUnit.MILLISECONDS);
    }

}
