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

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.logging.Level.FINE;

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
    private ScheduledExecutorService scheduler;
    private Clock clock;

    public MetricsUpdater(MeterRegistry registry, int maxThreads) {
        this(registry, maxThreads, "app_async_update_scrape_errors");
    }

    public MetricsUpdater(MeterRegistry registry, int maxThreads, String scrapeErrorsMetricName) {
        this(registry,
                Executors.newScheduledThreadPool(maxThreads, DaemonThreadNamingFactory.withPrefix("micrometer-metrics-updater")),
                scrapeErrorsMetricName,
                Clock.systemDefaultZone());
    }

    MetricsUpdater(MeterRegistry registry, ScheduledExecutorService scheduler, String scrapeErrorsMetricName, Clock clock) {
        this.clock = clock;
        this.scheduler = scheduler;

        Gauge.builder(scrapeErrorsMetricName, this::getScrapeErrors).register(registry);
    }

    double getScrapeErrors() {
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
        // Based on implementation in DiggConcurrent.ensureShutdown
        // https://github.com/digipost/digg/blob/0.34/src/main/java/no/digipost/DiggConcurrent.java#L136-L167
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(30, SECONDS)) {
                LOG.info("Digipost MetricsUpdater ScheduledExecutor is forcefully shut down as waiting for orderly termination took more than 30 seconds");
                scheduler.shutdownNow();
            } else {
                LOG.info("Digipost MetricsUpdater ScheduledExecutor was orderly shut down within the timeout of 30 seconds");
            }
        } catch (InterruptedException e) {
            String logMessageTemplate = "Interrupted while waiting for termination of Digipost MetricsUpdater ScheduledExecutor. %s: %s";
            if (LOG.isLoggable(FINE)) {
                LOG.log(FINE, e, () -> String.format(logMessageTemplate, e.getClass().getSimpleName(), e.getMessage()));
            } else {
                LOG.info(() -> String.format(logMessageTemplate, e.getClass().getSimpleName(), e.getMessage()));
            }
        }
    }

    /**
     * Alias for {@link #stop()} for Spring <a href="https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/context/annotation/Bean.html#destroyMethod--">@Bean</a> usage. This ensures that Spring automatically calls this method when destroying a bean.
     */
    public void shutdown() {
        stop();
    }

    public void registerAsyncUpdate(String updaterName, Duration updateInterval, Runnable setNewValues) {
        AsyncUpdater asyncUpdater = new AsyncUpdater(clock, updaterName, setNewValues, updateInterval);
        updaters.add(asyncUpdater);
        scheduler.scheduleAtFixedRate(asyncUpdater, 0, updateInterval.toMillis(), TimeUnit.MILLISECONDS);
    }

}
