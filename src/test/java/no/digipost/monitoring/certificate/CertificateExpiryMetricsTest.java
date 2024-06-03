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

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.search.RequiredSearch;
import io.micrometer.core.instrument.search.Search;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import no.digipost.monitoring.certificate.CertificateExpiryMetrics.ValidityStatus;
import no.digipost.time.ControllableClock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Stream;

import static no.digipost.monitoring.certificate.CertificateExpiryMetrics.ValidityStatus.EXPIRING;
import static no.digipost.monitoring.certificate.CertificateExpiryMetrics.ValidityStatus.INVALID;
import static no.digipost.monitoring.certificate.CertificateExpiryMetrics.ValidityStatus.VALID;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CertificateExpiryMetricsTest {

    private final KeyStore keyStore;
    private final List<MonitoredX509Certificate> monitoredX509CertificateList = new ArrayList<>();

    public CertificateExpiryMetricsTest() throws IOException, CertificateException, KeyStoreException, NoSuchAlgorithmException {
        this.keyStore = loadKeyStore();

        for (Enumeration<String> aliases = keyStore.aliases(); aliases.hasMoreElements(); ) {
            final String alias = aliases.nextElement();
            final Certificate certificate = keyStore.getCertificate(alias);
            
            monitoredX509CertificateList.add(new MonitoredX509Certificate((X509Certificate) certificate, alias));
        }
    }

    private static KeyStore loadKeyStore() throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException {
        KeyStore keyStore;
        try (InputStream is = CertificateExpiryMetricsTest.class.getResourceAsStream("/testing.p12")) {
            keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(is, "abcd1234".toCharArray());
        }
        return keyStore;
    }

    @Test
    void bindsGaugesCertExpiryForEachCertificate() {
        final Clock clock = ControllableClock.freezedAt(LocalDateTime.of(2022, Month.JUNE, 24, 10, 10));

        CertificateExpiryMetrics instance = new CertificateExpiryMetrics(monitoredX509CertificateList, clock);
        MeterRegistry registry = new SimpleMeterRegistry();
        instance.bindTo(registry);
        Search search = registry.find("cert_expiry");
        Collection<Gauge> gauges = search.gauges();
        assertEquals(monitoredX509CertificateList.size(), gauges.size());
    }

    @Test
    void bindsGaugesCertificateStatusForEachCertificate() {
        final Clock clock = ControllableClock.freezedAt(LocalDateTime.of(2022, Month.JUNE, 24, 10, 10));

        CertificateExpiryMetrics instance = new CertificateExpiryMetrics(monitoredX509CertificateList, clock);
        MeterRegistry registry = new SimpleMeterRegistry();
        instance.bindTo(registry);
        Search search = registry.find("certificates_status");
        Collection<Gauge> gauges = search.gauges();
        assertEquals(monitoredX509CertificateList.size(), gauges.size());
    }

    @Test
    void gaugeGetsExpectedExpiryTime() throws Exception {
        final Clock clock = ControllableClock.freezedAt(LocalDateTime.of(2022, Month.JUNE, 24, 10, 10));

        X509Certificate cert = (X509Certificate) keyStore.getCertificate("testing testing");
        CertificateExpiryMetrics instance = new CertificateExpiryMetrics(monitoredX509CertificateList, clock);
        MeterRegistry registry = new SimpleMeterRegistry();
        instance.bindTo(registry);
        RequiredSearch rs = registry.get("cert_expiry");
        rs.tag("alias", "testing testing");
        Gauge gauge = rs.gauge();
        long expected = (cert.getNotAfter().getTime() - clock.millis()) / 1000;
        long result = (long) gauge.value();
        assertEquals(expected, result);
    }

    static Stream<Arguments> gaugeCertificateStatusTesting() {
        return Stream.of(
                Arguments.of("testing testing", VALID, LocalDateTime.of(2022, Month.JUNE, 24, 10, 10)),
                Arguments.of("testing testing", EXPIRING, LocalDateTime.of(2023, Month.MAY, 24, 10, 10)),
                Arguments.of("testing testing", INVALID, LocalDateTime.of(2023, Month.JULY, 24, 10, 10)),
                Arguments.of("www.google.com (gts ca 1c3)", VALID, LocalDateTime.of(2022, Month.JUNE, 24, 10, 10)),
                Arguments.of("www.google.com (gts ca 1c3)", EXPIRING, LocalDateTime.of(2022, Month.JULY, 24, 10, 10)),
                Arguments.of("www.google.com (gts ca 1c3)", INVALID, LocalDateTime.of(2022, Month.SEPTEMBER, 24, 10, 10))
        );
    }

    @ParameterizedTest
    @MethodSource("gaugeCertificateStatusTesting")
    void gaugeGetsExpectedCertificateStatusValid(String certAlias, ValidityStatus status, LocalDateTime now) {
        final Clock clock = ControllableClock.freezedAt(now);

        CertificateExpiryMetrics instance = new CertificateExpiryMetrics(monitoredX509CertificateList, clock);
        MeterRegistry registry = new SimpleMeterRegistry();
        instance.bindTo(registry);
        RequiredSearch rs = registry.get("certificates_status");
        rs.tag("alias", certAlias);
        Gauge gauge = rs.gauge();
        long result = (long) gauge.value();
        assertEquals(status.code(), result);
    }
}
