# Claude Code instructions

This file is the Claude Code runtime entrypoint for this repository.

Treat `AGENTS.md` as the canonical shared baseline. Follow both files.

## Runtime files

- Shared baseline: `AGENTS.md`
- Claude project subagents: `.claude/agents/*.md`
- Copilot mirrors: `.github/agents/*.agent.md`
- Gemini mirrors: `.gemini/agents/*.md`

## Read first

- `AGENTS.md`
- `CLAUDE.md`
- `README.md`, if present
- `docs/`, if present
- the relevant build and dependency manifests
- CI and automation files already tracked in the repo

## Startup interview

- At the start of a fresh project/bootstrap session, ask the developer for:
  1. package name and project name
  2. Java version
  3. an optional starter prompt for the agent to begin working on the project
- If the user already supplied any of these, reuse them and ask only for the missing required items.
- Treat the answers as authoritative for repo identity and toolchain changes.
- Package/project answers must drive package paths, Gradle names and metadata, plugin identifiers, and related docs, config, and automation.
- Java version must drive the Gradle toolchain and any related docs, config, and automation.
- If a starter prompt is provided, continue into that work after capturing the interview answers.

## Fresh template plugin examples

Use this catalog when turning the fresh template into a real plugin. Keep the examples anonymous: rename, reshape, and adapt them for the current project instead of copying feature names or identifiers from anywhere else.

### 1. Build metadata and plugin descriptor

```kotlin
val identifier = "ExamplePlugin"
val location = "gg.moonrise.example"
val pluginVersion = "1.0.0"

configure<PaperPluginDescription> {
    name = identifier
    apiVersion = "1.21"
    version = pluginVersion
    main = "$location.ExamplePlugin"
    loader = "$location.loader.ExamplePluginLoader"
    load = BukkitPluginDescription.PluginLoadOrder.POSTWORLD

    serverDependencies {
        register("RequiredDependency") {
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
            required = true
        }
    }
}
```

Use this when wiring the plugin identity, main class, loader, and an optional hard dependency.

### 2. Plugin bootstrap and bean lifecycle

```java
public final class ExamplePlugin extends PaperPlugin {
}
```

Use this as the default main-class shape. `PaperPlugin` already handles Moss bootstrap, scheduler setup, MiniMessage setup, and Spring-driven listener registration, so no `onEnable` override is required unless the plugin has truly custom startup work.

### 3. Runtime library loader

```java
public final class ExamplePluginLoader extends PaperPluginLoader {

    @Override
    public void addLibraries(MavenLibraryResolver resolver) {
        resolver.addDependency(dependency("com.github.ben-manes.caffeine:caffeine:3.2.3"));
    }
}
```

Use this when a `compileOnly` dependency such as Caffeine is needed at runtime.

Keep the plugin loader class and descriptor entry even when `addLibraries` ends up empty. `PaperPluginLoader` performs base registrations through its superclass path, so the plugin still needs that loader hook to run.

### 4. Config model

```java
@Getter
@Configuration
public final class Config {

    private Message welcomeMessage = Message.of("<green>Welcome, <player>!</green>");
    private boolean enableVerboseLogging = false;
    private int actionCooldownSeconds = 10;
}
```

Use this for YAML-backed config with typed defaults instead of scattered string literals and magic numbers.

### 5. Config provider and reload flow

```java
@SpringComponent
@RequiredArgsConstructor
public final class ConfigProvider implements Reloadable {

    private final ExamplePlugin plugin;
    private Configuration<Config> configuration;

    @PostConstruct
    public void init() {
        this.configuration = Configuration.config(new File(plugin.getDataFolder(), "config.yml"), Config.class, builder -> {
            builder.setNameFormatter(NameFormatters.LOWER_KEBAB_CASE);
            builder.addSerializer(Message.class, new MessageSerializer());
            builder.inputNulls(true);
            builder.outputNulls(false);
            return builder;
        });
    }

    public Config get() {
        return configuration.get();
    }

    @Override
    public void reload() {
        configuration.reload();
    }
}
```

Use this when config loading needs builder customization, serializers, and clean participation in plugin reload flows.

### 6. Paper command

```java
@SpringComponent
@RequiredArgsConstructor
public final class ExampleCommand implements PaperCommand {

    private final ExamplePlugin plugin;

    @Command("example reload")
    @Permission("example.command.reload")
    public void reloadCommand(CommandSourceStack source) {
        plugin.reload();
        source.getSender().sendRichMessage("<green>Example plugin configuration reloaded.</green>");
    }
}
```

Use this for annotation-driven Paper commands that delegate real work to plugin services and reload flows.

### 7. Proxy or standalone command

```java
public final class ExampleProxyCommand extends Command {

    public ExampleProxyCommand() {
        super("exampleproxy", null, "example");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        sender.sendMessage("Attempting sync...");
    }
}
```

Use this on non-Paper runtimes that do not use the Paper command registry.

### 8. Listener or controller

```java
@SpringComponent
@RequiredArgsConstructor
public final class ExampleController implements Listener {

    private final ConfigProvider configProvider;
    private final SessionService sessionService;
    private final FormatService formatService;

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        sessionService.markOnline(player.getUniqueId());
        player.sendRichMessage(formatService.formatWelcome(configProvider.get(), player));
    }
}
```

Use this when one Spring-managed listener coordinates config access, state updates, and player-facing responses.

`@SpringComponent` listener beans are auto-registered by `PaperPlugin`, so the main class does not need to register them manually.

### 9. Menu or UI flow

```java
public final class ExampleMenu extends ChestMenu {

    public ExampleMenu(Player player) {
        super(player, "<green>Example</green>", 3);

        addButton(13, Button.builder()
            .item(viewer -> ItemBuilder.of(Material.EMERALD)
                .name("<green>Confirm")
                .build())
            .action((button, viewer, event) -> viewer.sendRichMessage("<gold>Confirmed"))
            .build());
    }
}
```

Use this for inventory-driven flows such as editors, selectors, inspectors, or confirmation screens.

### 10. Service and cache state

```java
@SpringComponent
public class SessionService {
    private final Set<UUID> onlinePlayers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Instant> lastActions = new ConcurrentHashMap<>();

    public void markOnline(UUID uuid) {
        onlinePlayers.add(uuid);
    }

    public void markOffline(UUID uuid) {
        onlinePlayers.remove(uuid);
        lastActions.remove(uuid);
    }

    public void markAction(UUID uuid, Instant instant) {
        lastActions.put(uuid, instant);
    }

    public Optional<Instant> lastAction(UUID uuid) {
        return Optional.ofNullable(lastActions.get(uuid));
    }
}
```

Use this when plugin state is transient and should stay owned by a dedicated service instead of static globals.

### 11. Messaging and rendering

```java
Message message = config.getWelcomeMessage();
Component component = message.asComponent(player,
        Placeholder.parsed("player", player.getName()));

player.sendMessage(component);
```

Use this when message templates, placeholders, and final rendered components should stay outside command and listener glue code.

### 12. Async work and temporary locks

```java
private void runAsyncThenRelease(UUID uuid, Consumer<ScheduledTask> task) {
    ACTIVE_ACTIONS.add(uuid);

    Scheduler.async().run(asyncTask -> {
        task.accept(asyncTask);
        Scheduler.sync().runDelayed(syncTask -> ACTIVE_ACTIONS.remove(uuid), Duration.ofSeconds(1));
    });
}
```

Use this when async work should finish first and a short temporary lock is needed before the next action can run.

### 13. JSON-backed settings service

```java
@SpringComponent
public class OperatorSettingsService {
    private final File file;
    private OperatorSettings settings;

    public OperatorSettingsService(ExamplePlugin plugin) {
        this.file = new File(plugin.getDataFolder(), "operator-settings.json");
        this.settings = JsonUtil.loadFromFile(file, OperatorSettings.class, GSON)
                .orElseGet(() -> new OperatorSettings(false, 0));
    }

    public void save(OperatorSettings settings) {
        this.settings = settings;
        Scheduler.async().run(task -> JsonUtil.saveToFile(file, this.settings, GSON));
    }
}
```

Use this when plugin state is not part of `config.yml` and should persist asynchronously as structured JSON.

### 14. Operator docs and command examples

```md
## Commands

- `/example reload` — reload plugin config
- `/example inspect <player>` — inspect current state for one player
- `/example toggle <player>` — toggle a feature for one player
- `/example status` — show the current plugin status
```

Use this when documenting the real operator workflows instead of leaving commands and permissions implicit.

## RTK command policy

- Use RTK for verbose shell-command workflows: `https://github.com/rtk-ai/rtk`
- Before depending on RTK in a session, verify the binary with `rtk --version` and `rtk gain`
- If RTK is missing, first try to install it autonomously when the runtime can do so safely and without changing user-global RTK setup unless explicitly asked
- If RTK is still unavailable after that, prompt the user to install RTK and restart the agent session before continuing with RTK-dependent shell workflows
- Prefer RTK-wrapped `git`, `gh`, shell file reads/searches, build, lint, test, package-manager, container, and log-heavy commands
- If RTK is available but unsupported for a command, fall back to standard shell usage but keep output tightly scoped
- Native read/search/edit tools still win when they fit better than shell execution
- Do not change user-global RTK setup unless explicitly asked

## Caveman install and mode

- Install by runtime:
  - GitHub Copilot: `npx skills add JuliusBrussee/caveman -a github-copilot`
  - Claude Code: `claude plugin marketplace add JuliusBrussee/caveman && claude plugin install caveman@caveman`
  - Codex: clone caveman repo, open `/plugins`, search `Caveman`, install
  - Gemini CLI: `gemini extensions install https://github.com/JuliusBrussee/caveman`
- Runtime install only adds the skill or plugin. Keep the tracked instruction files and runtime agent files in this repo so caveman stays active everywhere the runtime supports project-scoped config.
- Prefer persistent default when supported:
  - `export CAVEMAN_DEFAULT_MODE=ultra`
  - `~/.config/caveman/config.json`: `{ "defaultMode": "ultra" }`
- Repo default: `ultra`
- Never use `wenyan-lite`, `wenyan`, or `wenyan-ultra` in this repo
- Off only: `stop caveman` or `normal mode`

## Project agent layout

- GitHub Copilot project agents live in `.github/agents/*.agent.md`
- Claude project subagents live in `.claude/agents/*.md`
- Gemini project subagents live in `.gemini/agents/*.md`
- Codex CLI does not use a repo agent directory. It uses layered `AGENTS.md` and `AGENTS.override.md` files instead. Do not invent fake Codex subagent files.

## Local changelog workflow

- Changelog generation is local and human initiated. Do not delegate changelog drafting to GitHub Actions or any remote CI step.
- When the developer asks for a changelog, ask these questions first:
  1. what should the changelog contain
  2. how far back should it look
  3. any context, audience, exclusions, or emphasis
- If `.moonrise/changelog.config.json` exists, treat it as required context for product slug, tracked output path, and changelog emphasis.
- Inspect local git status, diff, and log using the developer-approved scope.
- Keep `title` and `summary` concise.
- After `title` and `summary`, organize changelog details into clear labeled areas such as `Add`, `Fix`, `Changed`, `Removed`, `Security`, `Docs`, `Internal`, or other explicit headings that fit the work.
- Moonrise publish payload still uses flat `highlights[]`, so write section heading entries like `Fixed:` or `Added:`, then item entries that repeat the same area keyword, such as `Fixed: corrected startup ordering` or `Added: module bootstrap checks`. Do not add a leading `* ` in changelog creation.
- Keep each section heading followed by item entries that repeat the same area keyword, for example `Fixed: ...`. Optional spacer entries like ` ` are allowed between sections when useful.
- Write `.moonrise/changelog/latest.json`.
- Set `"ready": true` only when the tracked changelog file is complete and intended for publication.
- Do not commit, push, or publish changelog unless the developer explicitly asks.


## Runtime notes

- Claude project subagents are markdown files with YAML frontmatter in `.claude/agents/`
- Keep Claude runtime prompts aligned with the shared baseline instead of drifting into Claude-only behavior
- All repo-level build, docs, design, Git, and communication rules live in `AGENTS.md`
