package dev.kehlchen.lib.core;

import dev.kehlchen.lib.api.Lifecycle;
import dev.kehlchen.lib.api.LifecycleState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Central bootstrap for deterministic module lifecycle management. */
public final class KehlLibBootstrap implements AutoCloseable {
    private final Map<String, Lifecycle> modules = new LinkedHashMap<>();
    private final Map<String, LifecycleState> states = new LinkedHashMap<>();
    private final List<Lifecycle> enabledInOrder = new ArrayList<>();
    private final ServiceRegistryImpl services = new ServiceRegistryImpl();
    private final ResourceManagerImpl resources = new ResourceManagerImpl();
    private final EventBusImpl eventBus = new EventBusImpl();
    private boolean closed;

    public void register(Lifecycle module) {
        ensureOpen();
        Objects.requireNonNull(module, "module");
        if (modules.putIfAbsent(module.name(), module) != null) {
            throw new IllegalArgumentException("Module is already registered: " + module.name());
        }
        states.put(module.name(), LifecycleState.REGISTERED);
    }

    public void enableAll() {
        ensureOpen();
        for (Lifecycle module : modules.values()) {
            if (states.get(module.name()) == LifecycleState.ENABLED) {
                continue;
            }
            try {
                module.onEnable();
                states.put(module.name(), LifecycleState.ENABLED);
                enabledInOrder.add(module);
            } catch (Throwable failure) {
                states.put(module.name(), LifecycleState.FAILED);
                disableEnabledModules();
                throw new IllegalStateException("Failed to enable module: " + module.name(), failure);
            }
        }
    }

    public void disableAll() {
        disableEnabledModules();
    }

    public LifecycleState state(String moduleName) {
        return states.get(moduleName);
    }

    public ServiceRegistryImpl services() {
        return services;
    }

    public ResourceManagerImpl resources() {
        return resources;
    }

    public EventBusImpl events() {
        return eventBus;
    }

    public List<Lifecycle> modules() {
        return Collections.unmodifiableList(new ArrayList<>(modules.values()));
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        Throwable failure = null;
        try {
            disableEnabledModules();
        } catch (Throwable error) {
            failure = error;
        }
        try {
            eventBus.close();
        } catch (Throwable error) {
            failure = error;
        }
        try {
            services.close();
        } catch (Throwable error) {
            if (failure == null) failure = error;
            else failure.addSuppressed(error);
        }
        try {
            resources.close();
        } catch (Throwable error) {
            if (failure == null) failure = error;
            else failure.addSuppressed(error);
        }
        if (failure != null) {
            throw new IllegalStateException("Failed to close KehlLib resources", failure);
        }
    }

    private void disableEnabledModules() {
        Throwable failure = null;
        for (int i = enabledInOrder.size() - 1; i >= 0; i--) {
            Lifecycle module = enabledInOrder.remove(i);
            try {
                module.onDisable();
                states.put(module.name(), LifecycleState.DISABLED);
            } catch (Throwable error) {
                states.put(module.name(), LifecycleState.FAILED);
                if (failure == null) {
                    failure = error;
                } else {
                    failure.addSuppressed(error);
                }
            }
        }
        if (failure != null) {
            throw new IllegalStateException("One or more modules failed to disable", failure);
        }
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("KehlLib bootstrap is closed");
        }
    }
}
