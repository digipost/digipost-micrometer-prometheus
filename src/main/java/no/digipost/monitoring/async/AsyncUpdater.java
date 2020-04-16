package no.digipost.monitoring.async;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

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
		} catch (Exception e) {
			LOG.log(Level.WARNING, "Unexpected exception in updater '" + updaterName + "' while updating metrics.", e);
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
