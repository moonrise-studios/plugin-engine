# plugin-engine

A Java 21 library for building Minecraft Paper, BungeeCord, and Velocity plugins with reusable building blocks for commands, scheduling, GUIs, messaging, universal toasts, configuration, and utility helpers.

Source repo: https://github.com/moonrise-studios/plugin-engine  
Organization: https://github.com/moonrise-studios

## What this library provides

`plugin-engine` is split into four modules:

| Module | Artifact | Purpose |
| --- | --- | --- |
| common | `gg.moonrise.engine:plugin-engine-common` | Platform-agnostic APIs and helpers (configuration, messages, command abstractions, utilities). |
| paper | `gg.moonrise.engine:plugin-engine-paper` | Paper-specific implementations (plugin base class, schedulers, command registration, GUI framework, dialogs and themed dialog menus, universal toasts, item builder, jobs). |
| bungeecord | `gg.moonrise.engine:plugin-engine-bungeecord` | BungeeCord-specific implementations (plugin base class, listener registration, Cloud command registration). |
| velocity | `gg.moonrise.engine:plugin-engine-velocity` | Velocity-specific implementations (plugin base class, lifecycle integration, audiences, Cloud command registration). |

## Compatibility

- Java 21
- Paper API `1.21.8-R0.1-SNAPSHOT` (for the `paper` module)
- Paper 26.2 command registration through Cloud Paper `2.0.0`
- BungeeCord API `1.21-R0.5-SNAPSHOT` (for the `bungeecord` module)
- Velocity API `3.4.0-SNAPSHOT` and Cloud Velocity `2.0.0-beta.10` (for the `velocity` module)
- Optional PacketEvents `2.12.x` (Java Edition toasts)
- Optional Geyser API `2.11.3-SNAPSHOT` or newer (native Bedrock toasts). `2.11.3` is currently a snapshot API line,
  shipped in Geyser builds from 2026-09-15 onward (the Minecraft Bedrock 26.50 support build). Older Geyser builds have
  no native toast API, and those players fall back to the Java advancement path automatically.
- Optional Floodgate API `2.2.x` (native Bedrock toasts when Geyser runs on a proxy or standalone)

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
    implementation("gg.moonrise.engine:plugin-engine-paper:1.9.0")
    // or: implementation("gg.moonrise.engine:plugin-engine-bungeecord:1.9.0")
    // or: implementation("gg.moonrise.engine:plugin-engine-velocity:1.9.0")
    // or: implementation("gg.moonrise.engine:plugin-engine-common:1.9.0")
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
        <version>1.9.0</version>
    </dependency>
    <!-- or: gg.moonrise.engine:plugin-engine-bungeecord:1.9.0 -->
    <!-- or: gg.moonrise.engine:plugin-engine-velocity:1.9.0 -->
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

## Quick start (Velocity plugins)

Velocity injects the proxy and plugin-owned data directory into your concrete plugin. Pass both to `VelocityPlugin` so Spring components can safely use `directory()` during initialization:

Your plugin build must also declare Velocity API as both `compileOnly` and `annotationProcessor` so Velocity generates its plugin metadata:

```kotlin
dependencies {
    implementation("gg.moonrise.engine:plugin-engine-velocity:1.9.0")
    implementation("gg.moonrise.moss:moss-velocity:1.2.3")
    implementation("org.springframework:spring-context:6.2.13")
    implementation("org.incendo:cloud-annotations:2.0.0")
    implementation("org.incendo:cloud-velocity:2.0.0-beta.10")
    compileOnly("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
    annotationProcessor("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
}
```

Shade and relocate non-platform runtime dependencies in your plugin JAR to avoid conflicts with other Velocity plugins.

```java
package com.example;

import com.google.inject.Inject;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import gg.moonrise.engine.velocity.VelocityPlugin;

import java.nio.file.Path;

@Plugin(id = "example", name = "Example", version = "1.0.0")
public final class ExampleVelocityPlugin extends VelocityPlugin {

    @Inject
    public ExampleVelocityPlugin(ProxyServer server, @DataDirectory Path dataDirectory) {
        super(server, dataDirectory);
    }
}
```

`VelocityPlugin` starts its Moss/Spring context during `ProxyInitializeEvent`, initializes MiniMessage utilities, auto-registers Moss Velocity listener beans, and exposes the shared `Plugin` contract. Listener beans implement `gg.moonrise.moss.velocity.spring.Listener` and use Velocity's `@Subscribe` methods.

## Optional Paper library loader

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

`velocity` includes `VelocityCommandRegistry`, which auto-discovers Spring beans implementing `VelocityCommand` plus shared `CloudArgument` parsers.

```java
package com.example.command;

import com.velocitypowered.api.command.CommandSource;
import gg.moonrise.engine.velocity.command.VelocityCommand;
import gg.moonrise.moss.spring.SpringComponent;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;

@SpringComponent
public final class ExampleVelocityCommand implements VelocityCommand {

    @Command("exampleproxy")
    @CommandDescription("Example Velocity command")
    public void example(CommandSource source) {

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

        // Menus default to 250 ms between handled button interactions.
        setInteractionCooldown(Duration.ofSeconds(1));

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

The interaction cooldown applies only when a registered button has a click action. Empty slots and
buttons without actions do not start it. Use `Duration.ZERO` to disable the cooldown for a menu.

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

### `ScrollingMenu`: vertical

`ScrollingMenu` renders buttons from one content list into a viewport. `ScrollDirection.VERTICAL` is the default. With a virtual shape, `x` cells consume content row by row. Shape width cannot exceed nine columns; shape height may exceed visible menu rows. Each line change moves the viewport by one row. Navigation button slots are reserved from content rendering.

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

### `ScrollingMenu`: horizontal

Set direction before configuring a horizontal shape. `x` cells consume content column by column. Shape height cannot exceed visible menu rows; shape width may exceed nine columns. Each line change moves the viewport by one column. This example uses a fourth inventory row for fixed left/right controls.

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

### `StaticScrollingMenu`: vertical

`StaticScrollingMenu` maps layout symbols to buttons. `ScrollDirection.VERTICAL` is the default. Layouts are exactly nine columns wide and may exceed visible menu rows. `setStaticLines(...)` always accepts layout row indexes; those rows remain pinned while non-static rows move vertically. Navigation controls may use a fallback symbol, such as `#`, when hidden.

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

### `StaticScrollingMenu`: horizontal

Set horizontal direction before configuring the layout. Horizontal static layouts may exceed nine columns but cannot exceed visible menu rows. `setStaticLines(...)` still accepts row indexes: static rows render their first nine columns without shifting, while non-static rows shift left or right one column per line change. In this three-row layout, rows `0` and `2` stay fixed; only row `1` scrolls.

```java
public final class HorizontalStaticScrollMenu extends StaticScrollingMenu {
    public HorizontalStaticScrollMenu(Player player) {
        super(player, "<green>Entries", 3);

        setScrollDirection(ScrollDirection.HORIZONTAL);
        setLayout(List.of(
                "u # # # # # # # d # # # # # # # # # #",
                "x x x x x x x x x x x x x x x x x x x",
                "# # # # # # # # # # # # # # # # # # #"
        ));
        setStaticLines(0, 2);
        setButton('x', Button.builder()
                .item(viewer -> ItemBuilder.of(Material.DIAMOND).name("<aqua>Entry").build())
                .build());
        setButton('#', Button.builder()
                .item(viewer -> ItemBuilder.of(Material.BLACK_STAINED_GLASS_PANE).name(" ").build())
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

### Themed dialog menus

`gg.moonrise.engine.paper.dialog.canvas` builds graphical dialog screens (tabs, rows of cards, player faces, themed
buttons) on top of Paper dialogs and a generated resource pack. Menu code describes screens from data; the engine
handles font glyphs, negative spacing, click callbacks, per-player sessions, and stale asynchronous results.

A menu is written once and works on every client. The engine picks a `DialogPresentation` per viewer when the menu
opens and renders the same `render(...)` description in it:

| Viewer | Presentation | What they see |
| --- | --- | --- |
| Java, theme pack loaded | `THEMED` | The themed canvas: panel, artwork, tabs, faces |
| Java, no theme pack | `NATIVE` | A vanilla dialog: the text as its body, every clickable element as a dialog button |
| Bedrock (Floodgate) | `BEDROCK` | A Floodgate simple form: the text as its content, every clickable element as a form button |

Every presentation runs the same click handlers, with the same stale-screen protection.

```java
public final class BalancesMenu extends DialogMenu<BalanceSnapshot> {

    private final BalanceService balances;

    public BalancesMenu(BalanceService balances, DialogTheme theme) {
        super("<aqua>Balances", theme);
        this.balances = balances;
    }

    @Override
    protected void render(DialogSession<BalanceSnapshot> session, DialogCanvas ui) {
        BalanceSnapshot state = session.data();

        ui.tabs(Currency.values(), state.currency())
                .label(Currency::displayName)
                .onSelect(currency -> session.load(() -> balances.loadSnapshot(session.playerId(), currency)));

        ui.centeredRows(DialogLayouts.rows(state.top(), 1, 3, 5, 7), (card, entry) -> {
            card.head(entry.playerId(), 16);
            card.text(Component.text(entry.playerName()));
            card.text(Component.text(entry.formattedBalance(), ui.theme().theme().accentColor()));
        });

        ui.footerText(Component.text(state.personalSummary()));
        ui.exitButton("<gold>Back", session::close);
    }
}

new BalancesMenu(balanceService, theme).open(player, snapshot);
```

A complete version lives in `paper/src/test/java/gg/moonrise/engine/paper/dialog/canvas/example`.
For one-off screens, `Dialogs.canvas(player).theme(theme).title(...).render((session, ui) -> ...).open()` skips the
subclass. `Dialogs.create(player)` remains the right tool for native forms with inputs.

Canvas components:

| Method | What it draws |
| --- | --- |
| `text`, `paragraph` | One line (shortened with `...`) or wrapped text; left, centre, or right aligned |
| `tabs(values, selected)` | A centred tab row with selected artwork, labels, tooltips, and `onSelect` |
| `centeredRows(rows, renderer)` | Rows of equal-width cards, each row centred (1-3-5-7 pyramids) |
| `card`, `head` | One centred card: faces (8, 16, or 24 px), text, images, buttons, links, and a `background` tile drawn behind it |
| `button(label, action)` | A themed body button; `.selected(true)`, `.width(...)`, `.tooltip(...)` |
| `link(label, url)` | A themed body button that opens a web page (also `card.link`) |
| `image(asset)`, `divider()`, `spacer(lines)` | Theme artwork and spacing |
| `footerText` | A line pinned below everything else |
| `exitButton(label, handler)` | The native exit button, which also receives Escape; ends the session first |
| `backButton(label, handler)` | The native exit button as a way back; the session stays open for the handler's next screen |

`DialogLayouts.rows(entries, 1, 3, 5, 7)` and `DialogLayouts.grid(entries, columns)` group entries for `centeredRows`;
a grid of cards holding one `card.button(...)` each is a button grid whose last row is centred, like vanilla dialogs.

#### Themes and the resource pack

A `DialogTheme` defines the canvas width, panel, colours, and named artwork (`tab`, `tab_selected`, `button`,
`button_selected`, `back`, `divider`). `DialogTheme.builder(namespace, id)` starts from a working default theme;
replace any asset with `ThemeAsset.fixed(...)`, `ThemeAsset.stretchable(image, leftInset, rightInset)`, or
`ThemeAsset.button(fill, border, height)`, and the panel with `ThemePanel.nineSlice(...)` or `ThemePanel.generated(...)`.

`theme.compiled()` generates the resource pack and the matching glyph metadata in one deterministic pass, so the
pack and the Java layout code never disagree. Export it during enable and deliver it with your existing pack workflow:

```java
DialogTheme theme = DialogTheme.builder("myplugin", "menus").width(440).build();
ResourcePackExport pack = theme.compiled().pack();

pack.writeDirectory(packSourceDir, false); // merge into an existing pack, keeping its pack.mcmeta
pack.syncDirectory(nexoExternalPack, false); // or own a dedicated folder: rewrites changes, deletes stale files
pack.writeZip(getDataPath().resolve("dialog-theme.zip")); // or host the ZIP yourself

player.sendResourcePacks(ResourcePackRequest.resourcePackRequest()
        .packs(pack.info(URI.create("https://cdn.example.com/dialog-theme.zip")))
        .required(true));
```

The pack contains namespaced textures, one font (`<namespace>:dialog/<id>`), and a
`dialog-theme.<namespace>.<id>.json` manifest with the build hash and pack id. The font key is baked into every
rendered component, so pick a namespace your pack pipeline keeps as-is; a post-processor that moves foreign
namespaces into its own breaks the themed screen while the vanilla fallback keeps working. `pack.mcmeta` declares formats from
1.21.6 (`63`) to `99`; change that with `packFormat(PackFormat.range(min, max))`. Adding or changing artwork needs a new
pack build; changing tabs, names, balances, or faces does not.

#### Sessions, loading, and closing

Each `open(player, data)` creates a `DialogSession`. A player has at most one canvas session; opening another one ends
the previous one with `REPLACED`. Sessions also end on quit, on `close()`, after the exit button, when the plugin
disables, and once `sessionTimeout()` (10 minutes by default) passes without a render. Expired sessions are closed,
including the player's screen, by a sweep that runs every 30 seconds.

- `session.load(supplier)` runs the supplier off-thread and shows the result on the player's thread. Each load gets a
  ticket; only the newest result is shown, so rapid tab clicks always end on the last tab.
- `session.loadAsync(...)` does the same for an existing `CompletableFuture`.
- `session.update(data)` and `session.refresh()` re-render immediately. A refresh sends a replacement dialog; native
  input values, focus, and scroll position are not preserved.
- Click actions only run for the screen they were rendered on, only for the session's player, and only while the
  session is open.
- `render(...)` should only read session data. Fetch from databases in `load`, and preload faces with
  `session.runtime().heads().preload(ids)`; an uncached face renders as a grey placeholder and is fetched in the background.

Override `onClose`, `onLoadFailure`, `sessionTimeout`, or `title` on `DialogMenu` to customise the lifecycle. `onClose`
and `onLoadFailure` run on the player's thread while the player is online.

#### Pack and client checks

`open` chooses the presentation like this:

1. Floodgate reports a Bedrock player: `BEDROCK`, or `BEDROCK_CLIENT` when `bedrockForms()` returns `false`.
2. PacketEvents reports a Java client older than 1.21.6: `UNSUPPORTED_CLIENT`.
3. `hasResourcePack(player)` is true, or `requiresResourcePack()` is false: `THEMED`.
4. Otherwise `NATIVE`, or `RESOURCE_PACK_NOT_LOADED` when `nativeFallback()` returns `false`.

When there is a reason instead of a presentation, `onUnavailable(player, data, reason)` runs, which sends a short
message by default.

`hasResourcePack` checks that the client reported `SUCCESSFULLY_LOADED` for one of `acceptedResourcePacks()`, by
default the theme's own pack id. When the theme is merged into a server pack whose id changes with every build, such
as one Nexo generates, override it:

```java
@Override
protected boolean hasResourcePack(Player player) {
    return runtime().packs().isAnyLoaded(player);
}
```

`nativeColumns()`, `nativeButtonWidth()`, and `nativeBodyWidth()` size the vanilla dialog. In it and in Bedrock forms,
artwork is left out, the selected tab is marked `» label «`, and cards with a face or a click action become one
button labelled with the card's text. Bedrock buttons show the face from `runtime.bedrockHeadImages(...)`, by default
`https://mc-heads.net/avatar/<uuid>/64`. Bedrock forms cannot open web pages, so links send the address to chat.
A Bedrock response consumes the form: after a click that shows no new screen, the engine sends the current one again,
and closing the form runs the exit or back button.

Bedrock form buttons can show images. Cards with a face show it from a web address; a card's icon shows when the
menu overrides `bedrockImage(asset)` with a Bedrock texture path. `BedrockPackExport.of(theme)` builds a
`.mcpack` with the theme's fixed artwork at `textures/dialog/<namespace>/<id>/<asset>.png`; install it where Geyser
loads packs (its `packs` folder) and return `pack.texturePath(asset)`. Bedrock players download it when they join, so a
new pack needs a Geyser restart and a reconnect.

Geyser does not turn forms into custom JSON UI: it sends the form as JSON and the Bedrock client draws it with its own
built-in form screen, a scrolling list of 32 px rows with an optional 32 x 32 image beside each button, labels of at
most two lines, and text above. The engine does not restyle that screen. Forms stack on the client, so a session
closes its unanswered form before it sends the next one or ends; that close also dismisses forms other plugins have
open for the player. Floodgate is used when it is enabled, since it reaches Geyser on a proxy too; otherwise Geyser on
this server (`Geyser-Spigot`) is used directly. Web images (player faces) show loading bars until they arrive.

Bedrock forms go through the Floodgate or Geyser API on the backend, and the old-client check through PacketEvents. A
plugin that shades the engine must declare `floodgate`, `Geyser-Spigot`, and `packetevents` as soft dependencies (in `paper-plugin.yml`, with `join-classpath: true`) so
it can see their APIs; Paper isolates plugin class loaders, so an installed plugin is not enough. Without that the
engine logs a warning once: Geyser translates the vanilla dialog for Bedrock players, and every Java client is
treated as new enough.

`PaperPlugin` registers the listener that tracks pack status and quits, and ends open sessions on disable. The shared
runtime schedules through the engine `Scheduler`, which only `PaperPlugin` initialises, so outside `PaperPlugin` install
a runtime backed by your own `DialogExecutor`:

```java
// onEnable, before any menu opens
DialogCanvasRuntime runtime = new DialogCanvasRuntime(executor);
DialogCanvasRuntime.install(runtime);
runtime.register(plugin);

// onDisable
runtime.shutdown();
```

#### Client behaviour to keep in mind

- With `exitButton`, Escape runs the exit handler. The session ends and the screen closes before the handler runs, so
  the handler can open any other dialog. With `backButton`, Escape closes the screen on the client and runs the
  handler; when the handler shows nothing new, the current screen is shown again. Without an exit button the dialog has no footer, and Escape closes the screen
  on the client without telling the server; the session then lives until it times out, the player quits, or another
  canvas opens.
- Every component is one text line; line breaks in `text`, labels, and the footer are replaced with spaces. Use
  `paragraph` for multi-line text.
- A themed screen that needs more lines than the theme's `maxLines`, or has a tab row or a row of cards too wide for
  the theme, is shown as the vanilla dialog instead, with a warning in the log once per session, so long data never
  leaves a player without a screen.
- `paragraph` keeps inline colours, decorations, and click events when it wraps; translatable and keybind parts are
  kept whole.
- Widths are exact for the vanilla ASCII characters. Other characters are estimated, and translatable components are
  measured by their key, so such text can shift its own line a few pixels off centre.
- Clicks are reliable on the text line a button's label sits on. Artwork drawn above or below that line may not be
  clickable on 1.21.6 to 1.21.8 clients, which hit-test one text line at a time.
- Tooltips and selected states are supported. Hover callbacks, dragging, and per-element animation are not.
- The vanilla dialog warning button next to the title cannot be removed.
- Layout is unit-tested against the vanilla font metrics, not in a running client. Check new themes in a real client
  at the GUI scales you support, including clicks after keyboard navigation.

## Toasts

`gg.moonrise.engine.paper.toast` sends achievement-style toast popups to Java Edition and Bedrock Edition players
through a single call. `Toasts` picks the right delivery path per player, so plugin code does not branch on platform.

```java
Toasts.send(player, "<green>Upgrade Successful!", "<white>Efficiency upgraded to <yellow>Level 4");

ToastResult result = Toasts.send(player, Toast.builder()
        .title(Message.of("<green>Quest Complete!"))
        .content("<white>You completed The Beginning!")
        .icon(Material.DIAMOND_PICKAXE)
        .frame(ToastFrame.GOAL)
        .javaLine(ToastJavaLine.BOTH)
        .build());
```

`title` and `content` accept a MiniMessage `String`, an Adventure `Component`, or a `Message`. `icon` accepts a
`Material` or an `ItemStack` and defaults to `PAPER`. `frame` is `TASK` (default), `GOAL`, or `CHALLENGE`. `javaLine`
is `TITLE` (default), `CONTENT`, `BOTH` (title and content joined by a space), or `TWO_LINES` (title and content as two
explicit lines). `ToastResult` reports which path ran: `BEDROCK`, `JAVA`, or `UNSUPPORTED`.

### Two custom lines on Java Edition

`TWO_LINES` gives Java Edition players two fully custom lines without a resource pack:

```java
Toasts.send(player, Toast.builder()
        .title("<gold>Quest Complete!")
        .content("<white>Construction Worker Quest")
        .frame(ToastFrame.CHALLENGE)
        .javaLine(ToastJavaLine.TWO_LINES)
        .build());
```

The client splits the advancement title at 125 px per line. When the split produces two or more lines, the toast shows
the fixed frame header (`Advancement Made!`, `Goal Reached!`, `Challenge Complete!`) for roughly the first 1.5 seconds,
fades it out, and then draws the custom lines in its place for the rest of the 5 second display. Forcing a line break
therefore makes both toast lines custom in the steady state.

Each line is still limited to 125 px and may wrap further, so keep both lines short. The frame keeps controlling the
sound and the colour of the brief header. The only way to change the header text itself is a resource-pack language
override of `advancements.toast.task`, `advancements.toast.goal`, or `advancements.toast.challenge`, which the engine
does not manage.

This behaviour is read from the client's advancement toast rendering and is covered by unit tests at the packet level.
It has not been verified against a live Java Edition client.

Routing works in this order:

1. If the Geyser API is present on the server and the player has a Geyser connection, a native Bedrock toast is sent
   through `GeyserConnection#sendToast`. That method arrived on the `2.11.3-SNAPSHOT` API line, which ships in Geyser
   builds from 2026-09-15 onward (the Minecraft Bedrock 26.50 support build); on older Geyser builds the engine detects
   the missing method and falls through to the Java advancement path instead.
2. Otherwise, if Floodgate is installed and the player is a Floodgate player, a native Bedrock `ToastRequestPacket` is
   encoded and handed to Floodgate, which forwards it over its plugin channel until Geyser injects it. This covers
   Geyser running on a proxy or standalone. The route is fire and forget: `BEDROCK` means the packet was handed off,
   not that the client displayed it.
3. Otherwise, if PacketEvents is installed, a temporary hidden fake advancement is sent and removed two ticks later.
4. Otherwise nothing is sent and `UNSUPPORTED` is returned. No exception is thrown.

All three integrations are optional.

| Platform | First line | Second line | Icon | Formatting |
| --- | --- | --- | --- | --- |
| Java | Fixed by the frame (`Advancement Made!`, `Goal Reached!`, `Challenge Complete!`), or with `javaLine = TWO_LINES` only for the first ~1.5 s before the custom lines replace it | One custom line, chosen by `javaLine`; two custom lines with `TWO_LINES` | Custom item icon | Full Adventure components |
| Bedrock | Custom | Custom | None | Legacy section-sign formatting; hex colours are downsampled |

When Geyser runs on a Velocity or BungeeCord proxy, or standalone, the backend has no Geyser API. If Floodgate is
installed on the backend (and on the proxy, which forwards the plugin channel), Bedrock players still get a real
two-line native toast through the Floodgate route. Without Floodgate on the backend they take the Java advancement
path instead; Geyser translates that into a Bedrock toast itself, so it still displays, but with the frame text as the
first line.

Floodgate logs one warning about an "unsafe part of the Floodgate api" the first time this route is used. That is
expected: the raw packet channel is the only way to send a native toast from a backend that has no Geyser API. The
engine calls it once and caches the handle.

The engine does not shade or download PacketEvents, Geyser, or Floodgate; install them as server plugins. Declare them
as optional descriptor dependencies so the consuming plugin can see their classes:

```kotlin
serverDependencies {
    register("packetevents") {
        load = PaperPluginDescription.RelativeLoadOrder.BEFORE
        required = false
        joinClasspath = true
    }
    register("Geyser-Spigot") {
        load = PaperPluginDescription.RelativeLoadOrder.BEFORE
        required = false
        joinClasspath = true
    }
    register("floodgate") {
        load = PaperPluginDescription.RelativeLoadOrder.BEFORE
        required = false
        joinClasspath = true
    }
}
```

`Toasts.send` is safe to call from any thread. The delayed advancement removal uses the engine `Scheduler`, so call it
after the plugin has enabled.

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
