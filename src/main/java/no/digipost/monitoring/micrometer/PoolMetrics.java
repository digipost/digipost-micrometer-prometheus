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
package no.digipost.monitoring.micrometer;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;

/**
 * Gauge metrics for a pool. This can be any type of pool: database connection pool, a thread pool, a http client pool et cetera.
 * <p>
 * USAGE:
 * new PoolMetrics("mydatabase", new MyDatabaseConnectionPoolStats()).bindTo(meterRegistry);
 * <p>
 * Beware that there is a weak reference for the collector object instance
 * in both this class and in the gauge underneath. Retain either the
 * DbStatusCollector instance or the DatabaseAvailabilityMetrics instance
 * or the instance will be GC-ed. If this happens the metric will report a NaN value
 * <p>
 * You will need to implement the PoolStats interface with your pool api.
 * 
 * * app_pool_connections_max
 * * app_pool_connections_total
 */
public class PoolMetrics implements MeterBinder {

    private final String name;
    private final PoolStats poolStats;

    /**
     * @param name      'name' will be a tag in the metric
     * @param poolStats instance of your implementation of PoolStats
     */
    public PoolMetrics(String name, PoolStats poolStats) {
        this.name = name;
        this.poolStats = poolStats;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        registry.gauge(
                "app_pool_connections_max"
                , Tags.of("name", this.name, "type", this.poolStats.type(), "implementation", this.poolStats.implementation())
                , this.poolStats
                , PoolStats::getMaxPoolSize
        );
        registry.gauge(
                "app_pool_connections_total"
                , Tags.of("name", this.name, "type", this.poolStats.type(), "implementation", this.poolStats.implementation())
                , this.poolStats
                , PoolStats::getNumberOfUsedConnections
        );
    }
}
