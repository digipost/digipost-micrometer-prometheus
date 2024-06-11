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

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import static java.util.function.Predicate.not;
import static no.digipost.monitoring.micrometer.KeyValueResolver.FROM_ENVIRONMENT_VARIABLES;
import static no.digipost.monitoring.micrometer.KeyValueResolver.FROM_SYSTEM_PROPERTIES;
import static no.digipost.monitoring.micrometer.KeyValueResolver.noValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.co.probablyfine.matchers.OptionalMatchers.contains;
import static uk.co.probablyfine.matchers.OptionalMatchers.empty;

class KeyValueResolverTest {

    private static final String KEY = KeyValueResolverTest.class.getName();

    @Test
    void resolveFromSystemProperties() {
        assertThat(KeyValueResolver.FROM_SYSTEM_PROPERTIES.tryResolveValue(KEY), empty());
        doWithSystemProperty(KEY, "hello",
                () -> assertThat(KeyValueResolver.FROM_SYSTEM_PROPERTIES.tryResolveValue(KEY), contains("hello")));
    }

    @Test
    void resolveFromEnvironmentVariables() {
        var envVariable = pickAnyExistingEnvironmentVariable();
        assertThat(KeyValueResolver.FROM_ENVIRONMENT_VARIABLES.tryResolveValue(envVariable.getKey()), contains(envVariable.getValue()));
    }

    @Test
    void environmentVariableOverriddenBySystemProperty() {
        var systemProperties = System.getProperties();
        var envVariable = pickAnExistingEnvironmentVariable(not(systemProperties::containsKey));

        var resolver = KeyValueResolver.inOrderOfPrecedence(FROM_SYSTEM_PROPERTIES, FROM_ENVIRONMENT_VARIABLES).andThen(Map::entry);

        assertThat(resolver.tryResolveValue(envVariable.getKey()), contains(envVariable));

        doWithSystemProperty(envVariable.getKey(), "overridden",
                () -> assertThat(resolver.tryResolveValue(envVariable.getKey()), contains(Map.entry(envVariable.getKey(), "overridden"))));
    }

    @Test
    void notAbleToResolveAnyValue() {
        assertThat(KeyValueResolver.inOrderOfPrecedence(noValue(), FROM_SYSTEM_PROPERTIES).tryResolveValue("%&()$# does not exist"), empty());
    }


    private static void doWithSystemProperty(String key, String value, Runnable operation) {
        try {
            System.setProperty(key, value);
            operation.run();
        } finally {
            System.clearProperty(key);
        }
    }


    private static Map.Entry<String, String> pickAnyExistingEnvironmentVariable() {
        return pickAnExistingEnvironmentVariable(key -> true);
    }

    private static Map.Entry<String, String> pickAnExistingEnvironmentVariable(Predicate<? super String> keyFilter) {
        return pickAnExistingEnvironmentVariable((key, value) -> keyFilter.test(key));
    }

    private static Map.Entry<String, String> pickAnExistingEnvironmentVariable(BiPredicate<? super String, ? super String> keyValueFilter) {
        var env = System.getenv();
        return env.entrySet().stream().filter(e -> keyValueFilter.test(e.getKey(), e.getValue())).findAny()
                .orElseThrow(() -> new NoSuchElementException("Unable to resolve an environment variable from " + env));
    }


}
