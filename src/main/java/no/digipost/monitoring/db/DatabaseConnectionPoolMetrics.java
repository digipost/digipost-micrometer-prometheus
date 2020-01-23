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
package no.digipost.monitoring.db;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;

/**
 * Gauge metrics for databaase connection pol
 * <p>
 * USAGE:
 * new DatabaseConnectionPoolMetrics("mydatabase", new MyPoolStats()).bindTo(meterRegistry);
 * <p>
 * Beware that there is a weak reference for the collector object instance
 * in both this class and in the gauge underneath. Retain either the
 * DbStatusCollector instance or the DatabaseAvailabilityMetrics instance
 * or the instance will be GC-ed. If this happens the metric will report a NaN value
 * <p>
 * You will need to implement the PoolStats interface with your
 * connection pool api.
 */
public class DatabaseConnectionPoolMetrics implements MeterBinder {

    private final String name;
    private final PoolStats poolStats;

    /**
     * @param name      'dbname' will be a tag in the metric
     * @param poolStats instance of your implementation of PoolStats
     */
    public DatabaseConnectionPoolMetrics(String name, PoolStats poolStats) {
        this.name = name;
        this.poolStats = poolStats;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        registry.gauge(
                "app_database_status"
                , Tags.of("name", "DATABASE_POOL_CONNECTIONS_MAX", "type", this.poolStats.type(), "dbname", this.name, "implementation", this.poolStats.implementation())
                , this.poolStats
                , PoolStats::getMaxPoolSize
        );
        registry.gauge(
                "app_database_status"
                , Tags.of("name", "DATABASE_POOL_CONNECTIONS_USAGE", "type", this.poolStats.type(), "dbname", this.name, "implementation", this.poolStats.implementation())
                , this.poolStats
                , PoolStats::getNumberOfUsedConnections
        );
    }
}
