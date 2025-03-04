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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.logging.Logger;

import static java.util.logging.Level.WARNING;

class AsyncUpdater implements Runnable {

    private static final Logger LOG = Logger.getLogger(AsyncUpdater.class.getName());
    private final Runnable updateNewValues;
    private final Duration updateInterval;
    private Clock clock;
    private String updaterName;
    private Instant lastUpdate;
    private boolean lastUpdateSuccessful;

    AsyncUpdater(Clock clock, String updaterName, Runnable updateNewValues, Duration updateInterval) {
        this.clock = clock;
        this.updaterName = updaterName;
        this.updateNewValues = updateNewValues;
        this.updateInterval = updateInterval;
        lastUpdate = clock.instant();
        lastUpdateSuccessful = true;
    }

    @Override
    public void run() {
        try {
            updateNewValues.run();
            lastUpdate = clock.instant();
            lastUpdateSuccessful = true;
        } catch (Throwable e) {
            LOG.log(WARNING,
                    "Unexpected exception in updater '" + updaterName + "' while updating metrics: " +
                    e.getClass().getSimpleName() + " " + e.getMessage(), e);
            lastUpdateSuccessful = false;
        }
    }

    boolean isStale() {
        Duration sinceLastUpdate = Duration.between(lastUpdate, clock.instant());
        return sinceLastUpdate.toMillis() > 2 * updateInterval.toMillis();
    }

    boolean isFailing() {
        return !lastUpdateSuccessful;
    }

    public String getName() {
        return updaterName;
    }

    public Instant getLastUpdate() {
        return lastUpdate;
    }
}
