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
package no.digipost.monitoring.servlet;

import io.micrometer.core.instrument.MeterRegistry;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

public class ActiveRequestsFilter implements Filter {
    final Map<UUID, RequestMetaInfo> activeRequests = new ConcurrentHashMap<>();
    private final Config config;

    public ActiveRequestsFilter(MeterRegistry registry, ActiveRequestsFilter.Config config) {
        this.config = config;
        registry.gauge("app_http_requests_active", activeRequests, Map::size);
        registry.gauge("app_http_requests_max", config.getMaxThreads());

        registry.gauge("app_http_requests_longrunning", emptyList(), activeRequests,
                (Map<UUID, RequestMetaInfo> activeRequests) -> getLongRunning(activeRequests, config.longRunningThreshold).size());
    }

    @Override
    public void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse, final FilterChain filterChain) throws IOException, ServletException {
        UUID requestId = UUID.randomUUID();
        RequestMetaInfo metaInfo = createRequestMetaInfo(servletRequest, Instant.now());
        try {
            activeRequests.put(requestId, metaInfo);
            filterChain.doFilter(servletRequest, servletResponse);
        } finally {
            activeRequests.remove(requestId);
        }
    }

    @Override
    public void init(FilterConfig filterConfig) {}

    @Override
    public void destroy() {}

    private boolean excludedRequest(RequestMetaInfo requestMetaInfo) {
        return config.longRunningExclusions.stream().anyMatch(p -> p.test(requestMetaInfo));
    }

    private List<RequestMetaInfo> getLongRunning(Map<UUID, RequestMetaInfo> activeRequests, Duration longRunningThreshold) {
        Instant deadline = Instant.now().minus(longRunningThreshold);
        return activeRequests.values().stream()
                .filter(m -> m.isOlderThan(deadline) && !excludedRequest(m))
                .collect(toList());
    }

    private static RequestMetaInfo createRequestMetaInfo(ServletRequest servletRequest, Instant now) {
        if (servletRequest instanceof HttpServletRequest) {
            HttpServletRequest httpReq = (HttpServletRequest) servletRequest;
            String path = httpReq.getRequestURI();
            String method = httpReq.getMethod();
            return new RequestMetaInfo(path, method, now);
        } else {
            return new RequestMetaInfo();
        }
    }


    public static class RequestMetaInfo {
        public final String path;
        public final String method;
        public final Instant requestTime;

        public RequestMetaInfo(String path, String method, Instant requestTime) {
            this.path = path;
            this.method = method;
            this.requestTime = requestTime;
        }

        public RequestMetaInfo() {
            this("?","?", Instant.now());
        }

        public boolean isOlderThan(Instant deadline) {
            return requestTime.isBefore(deadline);
        }

        @Override
        public String toString() {
            return requestTime.toString() + " " + method + " " + path;
        }
    }

    public static class Config {
        private Duration longRunningThreshold = Duration.ofMinutes(1);
        private int maxThreads;
        private List<Predicate<RequestMetaInfo>> longRunningExclusions = emptyList();

        private Config(int maxThreads) {
            this.maxThreads = maxThreads;
        }

        public static Config forMaxThreads(int maxThreads) {
            return new Config(maxThreads);
        }

        public Config longRunningThreshold(Duration longRunningThreshold) {
            this.longRunningThreshold = longRunningThreshold;
            return this;
        }

        public Config longRunningExclusions(List<Predicate<RequestMetaInfo>> longRunningExclusions) {
            this.longRunningExclusions = longRunningExclusions;
            return this;
        }

        public int getMaxThreads() {
            return maxThreads;
        }
    }
}
