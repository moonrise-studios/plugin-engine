# plugin-engine

A Java 21 library for building Minecraft Paper and BungeeCord plugins with reusable building blocks for commands, scheduling, GUIs, messaging, configuration, and utility helpers.

Source repo: https://github.com/moonrise-studios/plugin-engine  
Organization: https://github.com/moonrise-studios

## What this library provides

`plugin-engine` is split into three modules:

| Module | Artifact | Purpose |
| --- | --- | --- |
| common | `gg.moonrise.engine:plugin-engine-common` | Platform-agnostic APIs and helpers (configuration, messages, command abstractions, utilities). |
| paper | `gg.moonrise.engine:plugin-engine-paper` | Paper-specific implementations (plugin base class, schedulers, command registration, GUI framework, item builder, jobs). |
| bungeecord | `gg.moonrise.engine:plugin-engine-bungeecord` | BungeeCord-specific implementations (plugin base class, listener registration, Cloud command registration). |

## Compatibility

- Java 21
- Paper API `1.21.8-R0.1-SNAPSHOT` (for the `paper` module)
- BungeeCord API `1.21-R0.5-SNAPSHOT` (for the `bungeecord` module)

## Installation

Add Moonrise Studios Maven repository:

### Gradle (Kotlin DSL)

```kotlin
repositories {
    maven("https://repo.moonrise.gg/repository/maven-releases/")
    maven("https://repo.moonrise.gg/repository/maven-snapshots/")
}
```

Then add dependencies:

```kotlin
dependencies {
    implementation("gg.moonrise.engine:plugin-engine-paper:1.5.2")
    // or: implementation("gg.moonrise.engine:plugin-engine-bungeecord:1.5.2")
    // or: implementation("gg.moonrise.engine:plugin-engine-common:1.5.2")
}
```

### Maven

```xml
<repositories>
    <repository>
        <id>moonrise-releases</id>
        <url>https://repo.moonrise.gg/repository/maven-releases/</url>
    </repository>
    <repository>
        <id>moonrise-snapshots</id>
        <url>https://repo.moonrise.gg/repository/maven-snapshots/</url>
    </repository>
</repositories>
```

```xml
<dependencies>
    <dependency>
        <groupId>gg.moonrise.engine</groupId>
        <artifactId>plugin-engine-paper</artifactId>
        <version>1.5.2</version>
    </dependency>
    <!-- or: gg.moonrise.engine:plugin-engine-bungeecord:1.5.2 -->
</dependencies>
```

Use `-SNAPSHOT` versions when consuming snapshot builds.

## Quick start (Paper plugins)

### 1) Extend `PaperPlugin`

```java
package com.example;

import gg.moonrise.engine.paper.PaperPlugin;

public final class ExamplePlugin extends PaperPlugin {
}
```

`PaperPlugin` initializes scheduler and MiniMessage utilities for you and exposes the shared `Plugin` contract (`directory()`, `fetchBeans(...)`).

## Quick start (BungeeCord plugins)

```java
package com.example;

import gg.moonrise.engine.bungeecord.BungeePlugin;

public final class ExampleProxyPlugin extends BungeePlugin {
}
```

`BungeePlugin` initializes MiniMessage utilities, registers BungeeCord listener beans, and exposes the shared `Plugin` contract (`directory()`, `fetchBeans(...)`).

### 2) Optional Paper library loader

If you need extra runtime libraries, extend `PaperPluginLoader`:

```java
package com.example;

import gg.moonrise.engine.paper.loader.PaperPluginLoader;
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

import gg.moonrise.engine.paper.command.PaperCommand;
import gg.moonrise.moss.spring.SpringComponent;
import io.papermc.paper.command.brigadier.CommandSourceStack;
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

`bungeecord` includes `BungeeCordCommandRegistry`, which uses the same Cloud annotations flow for Spring beans implementing `BungeeCordCommand` and shared `CloudArgument` parsers.

```java
package com.example.command;

import gg.moonrise.engine.bungeecord.command.BungeeCordCommand;
import gg.moonrise.moss.spring.SpringComponent;
import net.md_5.bungee.api.CommandSender;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;

@SpringComponent
public final class ExampleProxyCommand implements BungeeCordCommand {

    @Command("exampleproxy")
    @CommandDescription("Example proxy command")
    public void example(CommandSender sender) {

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

Use `ChestMenu`, `PaginatedMenu`, `ScrollingMenu`, `StaticScrollingMenu`, or `HopperMenu` with `Button`. Prefer `MenuLayout` for new menus so slot structure, decorative fillers, content slots, and navigation buttons stay readable:

```java
public final class ExampleMenu extends ChestMenu {
    public ExampleMenu(Player player) {
        super(player, "<green>Example", 3);

        MenuLayout layout = MenuLayout.chest(
                "# # # # # # # # #",
                "# . . . . . . . #",
                "# # # # x # # # #"
        );

        addButtons(layout, '#', () -> Button.of(ItemBuilder.of(Material.GRAY_STAINED_GLASS_PANE)
                .name(" ")
                .build()));

        addButton(layout, 'x', Button.builder()
                .item(viewer -> ItemBuilder.of(Material.EMERALD)
                        .name("<green>Click me")
                        .build())
                .action((button, viewer, event) -> viewer.sendMessage(Component.text("Clicked")))
                .build());
    }
}
```

Menus are backed by Bukkit `InventoryHolder` instances and handled by `PlayerInventoryController`.

Paginated menus can use the same layout keys for content and navigation. Navigation buttons can also name a fallback key, such as `#`, so hidden previous/next controls render the same filler item as the rest of the layout. `setContentUnfiltered(...)` is the preferred large-list path because it does not render every content button up front; legacy `setContent(...)` still keeps eager empty-item filtering for older projects.

```java
public final class PlayersMenu extends PaginatedMenu {
    public PlayersMenu(Player player, List<PlayerProfile> profiles) {
        super(player, "<green>Players", 6);

        MenuLayout layout = MenuLayout.chest(
                "# # # # # # # # #",
                "# . . . . . . . #",
                "# . . . . . . . #",
                "# . . . . . . . #",
                "# . . . . . . . #",
                "# # # < # > # # #"
        );

        addButtons(layout, '#', () -> Button.of(ItemBuilder.of(Material.BLACK_STAINED_GLASS_PANE)
                .name(" ")
                .build()));

        setContentSlots(layout, '.');
        setPreviousPageButton(layout, '<', '#', Button.builder()
                .item(viewer -> ItemBuilder.of(Material.ARROW)
                        .name(hasPreviousPage() ? "<yellow>Previous page" : "<dark_gray>Previous page")
                        .build())
                .action((button, viewer, event) -> previousPage())
                .build());
        setNextPageButton(layout, '>', '#', Button.builder()
                .item(viewer -> ItemBuilder.of(Material.ARROW)
                        .name(hasNextPage() ? "<yellow>Next page" : "<dark_gray>Next page")
                        .build())
                .action((button, viewer, event) -> nextPage())
                .build());

        setContentUnfiltered(generateButtons(profiles, profile -> Button.builder()
                .item(viewer -> ItemBuilder.of(Material.PLAYER_HEAD)
                        .name("<green>" + profile.name())
                        .build())
                .action((button, viewer, event) -> openProfile(profile))
                .build()));
    }
}
```

`ScrollingMenu` renders a fixed viewport over a larger content list. Scrolling defaults to `ScrollDirection.VERTICAL`. Slot-list viewports preserve existing behavior: vertical mode uses configured order, horizontal mode orders slots by column, and `setLineLength(...)` overrides inferred line size. Virtual content shapes use formatted rows where whitespace is ignored and `x` marks content cells. Shapes may exceed visible height for vertical scrolling or nine columns for horizontal scrolling. Wire previous/next line buttons to `previousLine()` and `nextLine()`.

```java
public final class ExampleScrollMenu extends ScrollingMenu {
    public ExampleScrollMenu(Player player, List<Button> entries) {
        super(player, "<green>Entries", 5);

        setContentShape(List.of(
                "x x x x x x x x x",
                "x x x x x x x x x",
                "x x x x x x x x x",
                "x x x x x x x x x",
                "x x x x x x x x x",
                "x x x x x x x x x",
                "x x x x x x x x x",
                "x x x x x x x x x",
                "x x x x x x x x x"
        ));
        setPreviousLineButton(0, Button.builder()
                .item(viewer -> ItemBuilder.of(Material.ARROW).name("<yellow>Up").build())
                .action((button, viewer, event) -> previousLine())
                .build());
        setNextLineButton(36, Button.builder()
                .item(viewer -> ItemBuilder.of(Material.ARROW).name("<yellow>Down").build())
                .action((button, viewer, event) -> nextLine())
                .build());
        setContent(entries);
    }
}
```

Horizontal menus use the same content list and navigation API with left/right controls:

```java
public final class HorizontalScrollMenu extends ScrollingMenu {
    public HorizontalScrollMenu(Player player, List<Button> entries) {
        super(player, "<green>Entries", 4);

        setScrollDirection(ScrollDirection.HORIZONTAL);
        setContentShape(List.of(
                "x x x x x x x x x x x x x x x x x x x",
                "x x x x x x x x x x x x x x x x x x x",
                "x x x x x x x x x x x x x x x x x x x"
        ));
        setPreviousLineButton(27, Button.builder()
                .item(viewer -> ItemBuilder.of(Material.ARROW).name("<yellow>Left").build())
                .action((button, viewer, event) -> previousLine())
                .build());
        setNextLineButton(35, Button.builder()
                .item(viewer -> ItemBuilder.of(Material.ARROW).name("<yellow>Right").build())
                .action((button, viewer, event) -> nextLine())
                .build());
        setContent(entries);
    }
}
```

`StaticScrollingMenu` renders from a virtual layout. It defaults to vertical scrolling, where static line indexes pin layout rows. With `ScrollDirection.HORIZONTAL`, static line indexes pin layout columns and layouts may exceed nine columns. Navigation controls can name a fallback symbol, such as `#`, for the item to render when the control is hidden.

```java
public final class ExampleStaticScrollMenu extends StaticScrollingMenu {
    public ExampleStaticScrollMenu(Player player) {
        super(player, "<green>Entries", 6);

        setLayout(List.of(
                "# # # # i # # # #",
                "# # # # . # # # #",
                "# # # # . # # # #",
                "# # # # . # # # #",
                "# # # # . # # # #",
                "# # # # . # # # #",
                "# # a # # # b # #",
                "# # a # # # b # #",
                "# # a # # # b # #",
                "# # a # # # b # #",
                "# # a # # # b # #",
                "# # u # x # d # #"
        ));
        setStaticLines(0, 11);
        setButton('i', Button.builder()
                .item(viewer -> ItemBuilder.of(Material.BOOK).name("<yellow>Info").build())
                .build());
        setButton('.', Button.builder()
                .item(viewer -> ItemBuilder.of(Material.DIAMOND).name("<aqua>Entry").build())
                .build());
        setButton('#', Button.builder()
                .item(viewer -> ItemBuilder.of(Material.BLACK_STAINED_GLASS_PANE).name(" ").build())
                .build());
        setPreviousLineButton('u', '#', Button.builder()
                .item(viewer -> ItemBuilder.of(Material.ARROW).name("<yellow>Up").build())
                .action((button, viewer, event) -> previousLine())
                .build());
        setNextLineButton('d', '#', Button.builder()
                .item(viewer -> ItemBuilder.of(Material.ARROW).name("<yellow>Down").build())
                .action((button, viewer, event) -> nextLine())
                .build());
    }
}
```

Horizontal static layouts use the same format and pin selected columns:

```java
public final class HorizontalStaticScrollMenu extends StaticScrollingMenu {
    public HorizontalStaticScrollMenu(Player player) {
        super(player, "<green>Entries", 3);

        setScrollDirection(ScrollDirection.HORIZONTAL);
        setLayout(List.of(
                "u x x x x x x x x x x x x x x x x x d",
                "x x x x x x x x x x x x x x x x x x x",
                "x x x x x x x x x x x x x x x x x x x"
        ));
        setStaticLines(0, 18);
        setButton('x', Button.builder()
                .item(viewer -> ItemBuilder.of(Material.DIAMOND).name("<aqua>Entry").build())
                .build());
        setPreviousLineButton('u', Button.builder()
                .item(viewer -> ItemBuilder.of(Material.ARROW).name("<yellow>Left").build())
                .action((button, viewer, event) -> previousLine())
                .build());
        setNextLineButton('d', Button.builder()
                .item(viewer -> ItemBuilder.of(Material.ARROW).name("<yellow>Right").build())
                .action((button, viewer, event) -> nextLine())
                .build());
    }
}
```

For dynamic items, either set `Button.builder().refresh(ticks)` and let the controller refresh it while open, or call `button.notifyInventory(player)` after changing the backing state. Direct slot APIs such as `addButton(13, button)` and `setContent(...)` remain supported.

## Dialogs

Paper dialogs can be built with the fluent `Dialogs` wrapper. The wrapper uses Paper's callback-backed `DialogAction`, so no global listener or manual response map is needed for simple player prompts.

```java
Dialogs.create(player)
        .title("<green>Profile setup")
        .body("<gray>Pick the values to apply to your profile.")
        .input(Dialogs.text("nickname", "<yellow>Nickname")
                .initial(player.getName())
                .maxLength(16)
                .build())
        .input(Dialogs.numberRange("level", "<aqua>Level", 0f, 100f)
                .step(1f)
                .initial(1f)
                .build())
        .input(Dialogs.bool("public", "<gold>Public profile")
                .initial(true)
                .build())
        .submitButton("<green>Save", "<gray>Apply these values.", 120)
        .cancelButton("<red>Cancel")
        .whenComplete(player, output -> {
            String nickname = output.requireText("nickname");
            int level = Math.round(output.requireNumber("level"));
            boolean isPublic = output.bool("public", false);

            player.sendRichMessage(
                    "<green>Saved <nickname> at level <level> as <visibility>.",
                    Placeholder.parsed("nickname", nickname),
                    Placeholder.parsed("level", String.valueOf(level)),
                    Placeholder.parsed("visibility", isPublic ? "public" : "private")
            );
        });
```

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
- `UuidV7`: UUIDv7 generation, timestamp extraction, and 16-byte UUID encoding helpers
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

## AI runtime files

This repository now carries shared Moonrise runtime files for:

- `AGENTS.md`
- `CLAUDE.md`
- `GEMINI.md`
- `.github/agents/*.agent.md`
- `.claude/agents/*.md`
- `.gemini/agents/*.md`

## Local changelog workflow

Tracked files:

- `.moonrise/changelog.config.json`
- `.moonrise/changelog/latest.json`

Publish workflow:

- `.github/workflows/changelog.yml`

Publish helper:

- `.github/scripts/moonrise_changelog.py`

Expected developer flow:

1. make local changes
2. ask local agent to create changelog
3. answer:
   - what should changelog contain
   - how far back should it look
   - any context, exclusions, or emphasis
4. review `.moonrise/changelog/latest.json`
5. commit changelog with code changes
6. push watched branch
7. GitHub Actions publishes committed changelog to Moonrise App

Changelog format:

- keep normal `title` and `summary`
- use `highlights[]` as ordered list entries with section headings and bullet lines
- example:
  - `Fixed:`
  - `* corrected scheduler behavior for plugin consumers`
  - `* tightened command registration notes`
  - ` `
  - `Added:`
  - `* added clearer packaging guidance for consumers`
