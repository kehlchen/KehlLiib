# Getting Started

## Install as a Gradle dependency

Use `implementation` for the module containing the implementation classes. External plugins generally need `api` for contracts and `core` for Paper implementations.

```kotlin
repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    implementation("dev.kehlchen:kehllib-core:1.0")
    compileOnly("io.papermc.paper:paper-api:26.3.build.+")
}
```

Publishing the standalone plugin is supported as `dev.kehlchen:kehllib:1.0`. A plugin normally does not need to embed `core` if it declares the standalone plugin as a dependency.

```kotlin
dependencies {
    implementation("dev.kehlchen:kehllib-api:1.0")
    compileOnly("dev.kehlchen:kehllib:1.0")
}
```

Optional modules are separate:

```kotlin
dependencies {
    implementation("dev.kehlchen:kehllib-integration-redis:1.0")
    implementation("dev.kehlchen:kehllib-integration-vault:1.0")
    implementation("dev.kehlchen:kehllib-integration-bungee:1.0")
}
```

The coordinates above match the project publication names and version `1.0`.

## Local Maven development

```powershell
.\gradlew.bat publishToMavenLocal
```

Then add `mavenLocal()` to your consuming project.

## Initialize an embedded runtime

```java
public final class ExamplePlugin extends JavaPlugin {
    private KehlLibBootstrap runtime;

    @Override
    public void onEnable() {
        runtime = new KehlLibBootstrap();
        Localization localization =
            new Localization(getDataFolder().toPath(), "en");
        PaperScheduler scheduler = new PaperScheduler(this);
        PaperPlayerService players =
            new PaperPlayerService(this, scheduler);

        runtime.register(scheduler);
        runtime.register(players);
        runtime.services().register(Scheduler.class, scheduler);
        runtime.services().register(PlayerService.class, players);
        runtime.enableAll();
    }

    @Override
    public void onDisable() {
        if (runtime != null) {
            runtime.close();
            runtime = null;
        }
    }
}
```

Register every lifecycle module before `enableAll()`. Register public interface implementations only after constructing the implementation. Register created `Database`, `Storage`, and `Cache` instances with the runtime resource manager because these resources are not automatically owned by an embedded bootstrap.

## Use the standalone plugin

1. Build `plugin`: `.\gradlew.bat :plugin:build`.
2. Copy `plugin/build/libs/kehllib-1.0.jar` to the server `plugins` directory.
3. Start a Paper or Purpur server.
4. In another plugin, add KehlLib as a compile dependency and declare `depend: [KehlLib]` or `softdepend` according to whether it is required.

At runtime, obtain the standalone bootstrap through `KehlLibPlugin.runtime()`, or access registered interface services through the Bukkit plugin. Do not create a second KehlLib bootstrap in a plugin that depends on the standalone plugin.
