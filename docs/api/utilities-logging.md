# Utilities and Logging

## Utilities

The `dev.kehlchen.lib.core.util` package contains small helpers:

- `StringUtils`: string normalization and bounded text operations
- `NumberUtils`: numeric conversion and range operations
- `CollectionUtils`: collection helpers
- `UUIDUtils`: UUID parsing and conversion
- `DurationUtils` and `TimeUtils`: duration/time formatting
- `MathUtils`: numeric math helpers
- `ColorUtils`: color conversion and validation
- `ValidationUtils` and `Preconditions`: argument and state validation
- `VersionUtils`: version parsing/comparison
- `FileUtils`: directory/file operations
- `ReflectionUtils`: reflection helpers
- `BlockUtils`, `LocationUtils`, `ItemUtils`, and `EntityUtils`: Bukkit object helpers
- `SafeExecution`: guarded operation execution

Use each helper through its public static methods and inspect the implementation or IDE signature for exact overloads. Utility helpers do not change Bukkit thread requirements: callers remain responsible for Bukkit object ownership.

## Logging

The core logging package provides `KehlLibLogger`, `LoggerManager`, `LogLevel`, `StructuredLogEntry`, `PerformanceTimer`, and `ExceptionFormatter`.

Use structured data rather than concatenating secrets or player-supplied values into log lines:

```java
KehlLibLogger logger =
    new KehlLibLogger(plugin.getLogger(), "example");
logger.info("Database refresh completed");
```

`LogLevel` controls the configured level. `LoggerManager` owns named logger instances. `PerformanceTimer` measures elapsed time. `ExceptionFormatter` provides exception formatting.

Avoid logging credentials, Redis passwords, Vault values, webhook URLs with secrets, or full external response bodies by default.
