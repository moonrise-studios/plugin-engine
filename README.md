# plugin-engine

A Java 21 library for building Minecraft Paper plugins with reusable building blocks for commands, scheduling, GUIs, messaging, configuration, and utility helpers.

## What this library provides

`plugin-engine` is split into two modules:

| Module | Artifact | Purpose |
| --- | --- | --- |
| common | `games.negative.engine:plugin-engine-common` | Platform-agnostic APIs and helpers (configuration, messages, command abstractions, utilities). |
| paper | `games.negative.engine:plugin-engine-paper` | Paper-specific implementations (plugin base class, schedulers, command registration, GUI framework, item builder, jobs). |

## Compatibility

- Java 21
- Paper API `1.21.8-R0.1-SNAPSHOT` (for the `paper` module)

## Installation

Add the Negative Games Maven repository:

### Gradle (Kotlin DSL)

```kotlin
repositories {
    maven("https://repo.negative.games/repository/maven-releases/")
    maven("https://repo.negative.games/repository/maven-snapshots/")
}
```

Then add dependencies:

```kotlin
dependencies {
    implementation("games.negative.engine:plugin-engine-paper:1.0.0")
    // or: implementation("games.negative.engine:plugin-engine-common:1.0.0")
}
```

### Maven

```xml
<repositories>
    <repository>
        <id>negative-games-releases</id>
        <url>https://repo.negative.games/repository/maven-releases/</url>
    </repository>
    <repository>
        <id>negative-games-snapshots</id>
        <url>https://repo.negative.games/repository/maven-snapshots/</url>
    </repository>
</repositories>
```

```xml
<dependencies>
    <dependency>
        <groupId>games.negative.engine</groupId>
        <artifactId>plugin-engine-paper</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>
```

Use `-SNAPSHOT` versions when consuming snapshot builds.

## Quick start (Paper plugins)

### 1) Extend `PaperPlugin`

```java
package com.example;

import games.negative.engine.paper.PaperPlugin;

public final class ExamplePlugin extends PaperPlugin {
}
```

`PaperPlugin` initializes scheduler and MiniMessage utilities for you and exposes the shared `Plugin` contract (`directory()`, `fetchBeans(...)`).

### 2) Optional: custom library loader

If you need extra runtime libraries, extend `PaperPluginLoader`:

```java
package com.example;

import games.negative.engine.paper.loader.PaperPluginLoader;
import org.eclipse.aether.graph.Dependency;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;

public final class ExamplePluginLoader extends PaperPluginLoader {
    @Override
    public void addLibraries(MavenLibraryResolver resolver) {
        Dependency dep = dependency("com.example:example-lib:1.2.3");
        resolver.addDependency(dep);
    }
}
```

## Commands

`paper` includes `PaperCommandRegistry`, which auto-discovers Spring beans implementing `CloudCommand` and `CloudArgument`.

```java
package com.example.command;

import games.negative.engine.command.CloudCommand;
import games.negative.moss.spring.SpringComponent;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;

@SpringComponent
public final class ExampleCommand implements PaperCommand {

    @Command("example")
    @CommandDescription("Example command")
    public void example(CommandSourceStack source) {
        
    }
}
```

## Scheduling and jobs

Use the static `Scheduler` accessors:

```java
Scheduler.sync().run(task -> {
    // Main-thread/global region work
});

Scheduler.async().run(task -> {
    // Async work
});

Scheduler.entity(player).execute(() -> {
    // Entity-thread safe work
}, 1L);
```

For recurring background logic, create beans implementing `SyncJob` or `AsyncJob`; `JobScheduler` auto-registers them on enable:

```java
@SpringComponent
public final class AnnounceJob implements SyncJob {
    @Override
    public Duration interval() {
        return Duration.ofSeconds(30);
    }

    @Override
    public void tick(ScheduledTask task) {
        // Repeating logic
    }
}
```

## GUI framework

Use `ChestMenu`, `PaginatedMenu`, or `HopperMenu` with `Button`:

```java
public final class ExampleMenu extends ChestMenu {
    public ExampleMenu(Player player) {
        super(player, "<green>Example", 3);

        addButton(13, Button.builder()
                .item(viewer -> ItemBuilder.of(Material.EMERALD)
                        .name("<green>Click me")
                        .build())
                .action((button, viewer, event) -> viewer.sendMessage(Component.text("Clicked")))
                .build());
    }
}
```

Menus are cached per-player and handled by `PlayerInventoryController`.

## Messages and placeholders

`Message` and `MiniMessageUtil` support MiniMessage formatting plus PlaceholderAPI integration (when present):

```java
Message.of("<gold>Hello, <name>!")
        .send(player, Placeholder.parsed("name", player.getName()));
```

`PaperLocalizationPlatform` provides PlaceholderAPI and relational placeholder parsing for Paper audiences.

## Configuration

Use `Configuration<T>` with ConfigLib-backed YAML storage:

```java
public final class ExampleConfig {
    public String prefix = "<gray>[Example]</gray>";
}

Configuration<ExampleConfig> config = Configuration.config(
        plugin.directory().resolve("config.yml").toFile(),
        ExampleConfig.class
);

String prefix = config.get().prefix;
```

## Item building

`ItemBuilder` simplifies `ItemStack` creation:

```java
ItemStack stack = ItemBuilder.of(Material.DIAMOND_SWORD)
        .name("<aqua>Starter Sword")
        .lore(List.of("<gray>Given on join"))
        .unbreakable(true)
        .glowing(true)
        .build();
```

## Utilities in `common`

- `TimeUtil`: parse/format durations (`1d2h30m`, `H:MM:SS`, etc.)
- `NumberUtil`: decimal formatting, ordinals (`1st`, `2nd`), condensed numbers (`1.2M`)
- `AABB` (paper): simple axis-aligned bounding box representation

## Build and publish

From repository root:

```bash
./gradlew clean build
```

Publishing (used by CI for `release` / `snapshot` branches):

```bash
./gradlew clean publish -PisRelease=true   # releases repo
./gradlew clean publish -PisRelease=false  # snapshots repo
```

