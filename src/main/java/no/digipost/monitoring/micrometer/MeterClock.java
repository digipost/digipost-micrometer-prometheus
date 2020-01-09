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
package no.digipost.monitoring.micrometer;

import io.micrometer.core.instrument.Clock;

import java.time.Instant;

/**
 * If you want to show a timestamp for when a specific 
 * event occured, This provides an easy to use interface.
 * 
 * usage:
 * // You need the instance
 * MeterClock last_processed_notification_time = last_processed_notification_time = new MeterClock(systemClock);
 * 
 * // Register your instance and value-function to a metric
 * Metrics.more().timeGauge(
 * 			"app_last_processed"
 * 			, Tags.of("file", "notifications")
 * 			, last_processed_notification_time
 * 			, TimeUnit.SECONDS
 * 			, MeterClock::wallTime
 * 		);
 * 
 * [...]
 * // touch your ticker.
 * last_processed_notification_time.tick();
 */
public class MeterClock implements Clock {

	private java.time.Clock clock;
	private Instant state;

	public MeterClock(java.time.Clock clock) {
		this.clock = clock;
	}

	public void tick() {
		this.state = clock.instant();
	}

	@Override
	public long wallTime() {
		if (this.state == null) return 0L;
		return state.getEpochSecond();
	}

	@Override
	public long monotonicTime() {
		if (this.state == null) return 0L;
		return state.getEpochSecond() * 1_000_000_000L + state.getNano();
	}
}
