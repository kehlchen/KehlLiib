package dev.kehlchen.lib.core;

import dev.kehlchen.lib.api.Lifecycle;
import dev.kehlchen.lib.api.LifecycleState;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KehlLibBootstrapTest {
    private record TestModule(String name, List<String> calls, boolean failOnEnable) implements Lifecycle {
        @Override
        public String version() {
            return "test";
        }

        @Override
        public void onEnable() {
            calls.add(name + ":enable");
            if (failOnEnable) {
                throw new IllegalStateException("enable failure");
            }
        }

        @Override
        public void onDisable() {
            calls.add(name + ":disable");
        }
    }

    @Test
    void disablesModulesInReverseOrder() {
        List<String> calls = new ArrayList<>();
        KehlLibBootstrap bootstrap = new KehlLibBootstrap();
        bootstrap.register(new TestModule("one", calls, false));
        bootstrap.register(new TestModule("two", calls, false));

        bootstrap.enableAll();
        bootstrap.disableAll();

        assertEquals(List.of("one:enable", "two:enable", "two:disable", "one:disable"), calls);
        assertEquals(LifecycleState.DISABLED, bootstrap.state("one"));
        assertEquals(LifecycleState.DISABLED, bootstrap.state("two"));
    }

    @Test
    void failedEnableCleansAlreadyEnabledModules() {
        List<String> calls = new ArrayList<>();
        KehlLibBootstrap bootstrap = new KehlLibBootstrap();
        bootstrap.register(new TestModule("good", calls, false));
        bootstrap.register(new TestModule("bad", calls, true));

        assertThrows(IllegalStateException.class, bootstrap::enableAll);

        assertEquals(List.of("good:enable", "bad:enable", "good:disable"), calls);
        assertEquals(LifecycleState.DISABLED, bootstrap.state("good"));
        assertEquals(LifecycleState.FAILED, bootstrap.state("bad"));
    }
}
