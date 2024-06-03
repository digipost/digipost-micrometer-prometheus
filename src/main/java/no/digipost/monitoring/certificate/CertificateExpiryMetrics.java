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
package no.digipost.monitoring.certificate;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.MeterBinder;

import java.security.KeyStoreException;
import java.security.cert.X509Certificate;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.Arrays.asList;

/**
 * Micrometer {@link MeterBinder} registering gauges for Java key store certificates.
 *
 * <p>
 * Each certificate gauge keeps track of seconds until certificate expiry. It
 * also has some useful tags such as keystore alias, X509 certificate fields as
 * issuer, subject, notBefore and notAfter.
 */
public final class CertificateExpiryMetrics implements MeterBinder {

    public static final int DEFAULT_DAYS_TO_EXPIRY_WARN_THRESHOLD = 60;

    private final List<MonitoredX509Certificate> monitoredX509Certificates;
    private final Clock clock;
    private final int daysToExpiryWarnThreshold;

    /**
     * Create a new meter binder.
     *
     * @param monitoredX509Certificates key store containing certificates to monitor.
     */
    public CertificateExpiryMetrics(List<MonitoredX509Certificate> monitoredX509Certificates, Clock clock) {
        this(monitoredX509Certificates, clock, DEFAULT_DAYS_TO_EXPIRY_WARN_THRESHOLD);
    }

    /**
     * Create a new meter binder setting days to warn
     *
     * @param monitoredX509Certificates key store containing certificates to monitor.
     * @param clock The time of the server
     */
    public CertificateExpiryMetrics(List<MonitoredX509Certificate> monitoredX509Certificates, Clock clock, int daysToExpiryWarnThreshold) {
        this.clock                     = clock;
        this.daysToExpiryWarnThreshold = daysToExpiryWarnThreshold;
        this.monitoredX509Certificates = Objects.requireNonNull(monitoredX509Certificates, "monitoredX509Certificates can not be null");
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        try {
            for (MonitoredX509Certificate cert : monitoredX509Certificates) {
                registerGauge(registry, cert);
            }
        } catch (KeyStoreException e) {
            throw new IllegalStateException(e);
        }

    }

    private void registerGauge(MeterRegistry registry, MonitoredX509Certificate monitoredX509Certificate) throws KeyStoreException {
        X509Certificate cert = monitoredX509Certificate.certificate;
        List<Tag> tags = asList(
                Tag.of("alias", monitoredX509Certificate.description.orElse(cert.getSerialNumber().toString())),
                Tag.of("issuer", cert.getIssuerX500Principal().getName()),
                Tag.of("subject", cert.getSubjectX500Principal().getName()),
                Tag.of("notAfter", cert.getNotAfter().toString()),
                Tag.of("notBefore", cert.getNotBefore().toString())
        );
        registry.gauge(
                "cert_expiry", tags, cert,
                c -> Duration.between(Instant.now(clock), c.getNotAfter().toInstant()).getSeconds()
        );
        registry.gauge("certificates_status", tags, cert,
                c -> {
                    final long daysToExpiry = DAYS.between(LocalDate.now(clock), LocalDateTime.ofInstant(c.getNotAfter().toInstant(), clock.getZone()).toLocalDate());
                    if (daysToExpiry > this.daysToExpiryWarnThreshold) {
                        return ValidityStatus.VALID.code();
                    } else if (daysToExpiry <= 0) {
                        return ValidityStatus.INVALID.code();
                    } else {
                        return ValidityStatus.EXPIRING.code();
                    }
                }
        );
    }

    enum ValidityStatus {
        INVALID(-1), EXPIRING(0), VALID(1);

        private int i;

        ValidityStatus(int i) {
            this.i = i;
        }

        public int code() {
            return i;
        }
    }

}
