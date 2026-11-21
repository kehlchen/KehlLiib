# Lifecycle and Services

## Lifecycle interface

Every lifecycle module implements:

```java
String name();
String version();
void onEnable();
void onDisable();
```

The bootstrap records `REGISTERED`, `ENABLED`, `FAILED`, or `DISABLED` state through `LifecycleState`. Modules are enabled in registration order and disabled in reverse enable order.

## Accessing services

```java
Scheduler scheduler =
    runtime.services().getRequired(Scheduler.class);
```

Use `get(...)` when absence is expected:

```java
EconomyProvider economy =
    runtime.services().get(EconomyProvider.class).orElse(null);
```

Optional integrations should not be assumed available merely because an integration module is in the build. It must be registered and enabled.

## Registering an integration

```java
IntegrationRegistry integrations =
    runtime.services().getRequired(IntegrationRegistry.class);

integrations.register(myIntegration);
```

`IntegrationRegistry` is lifecycle-managed and accepts implementations of `Integration`.

## Resource cleanup

Register long-lived or externally created resources:

```java
Database database = databases.create(config);
runtime.resources().register(database);
```

Resource shutdown order and failure aggregation are owned by `ResourceManagerImpl`. Always close plugin-owned Bootstrap instances from `onDisable()`.
