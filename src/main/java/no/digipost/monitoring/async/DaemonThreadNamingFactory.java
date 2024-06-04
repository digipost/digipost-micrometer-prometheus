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

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * A {@link ThreadFactory} which names threads with a custom base name,
 * and marks new threads as daemon threads as well.
 */
class DaemonThreadNamingFactory implements ThreadFactory {
    private final ThreadFactory backingFactory = Executors.defaultThreadFactory();
    private final AtomicLong threadNum = new AtomicLong(0);
    private final Function<Long, String> namingScheme;

    /**
     * Create a thread factory based on Executors.defaultThreadFactory(), but naming the created
     * threads using the given threadBaseName and an incrementing number for each thread created by
     * the factory. The threads will have names on the form threadBaseName-N.
     *
     * All threads will be daemon threads.
     *
     * @param threadBaseName the base name for threads created by the factory.
     */
    public DaemonThreadNamingFactory(String threadBaseName) {
        if (threadBaseName == null || threadBaseName.isEmpty()) {
            throw new IllegalArgumentException("missing thread base name (null or empty)");
        }

        this.namingScheme = threadNum -> threadBaseName + "-" + threadNum;
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread newThread = backingFactory.newThread(r);
        newThread.setName(namingScheme.apply(threadNum.incrementAndGet()));
        newThread.setDaemon(true);

        return newThread;
    }

    public static DaemonThreadNamingFactory withPrefix(String threadBaseName) {
        return new DaemonThreadNamingFactory(threadBaseName);
    }
}
