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
package no.digipost.monitoring.prometheus;

import com.sun.net.httpserver.HttpServer;
import io.micrometer.prometheus.PrometheusMeterRegistry;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.function.BiConsumer;

public class SimplePrometheusServer {

    private static final String METRICS_PATH = "/metrics";
    private BiConsumer<String, Object> infoLogger;

    public SimplePrometheusServer(BiConsumer<String, Object> infoLogger) {
        this.infoLogger = infoLogger;
    }

    public void startMetricsServer(final PrometheusMeterRegistry prometheusContext, final int prometheusPort) {
        // https://micrometer.io/docs/registry/prometheus
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(prometheusPort), 0);
            server.createContext(METRICS_PATH, httpExchange -> {
                String response = prometheusContext.scrape();
                httpExchange.sendResponseHeaders(200, response.getBytes().length);
                try (OutputStream os = httpExchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            });

            new Thread(server::start).start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> server.stop(2)));

            infoLogger.accept("Started Prometheus metrics endpoint server on port {}", prometheusPort);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
