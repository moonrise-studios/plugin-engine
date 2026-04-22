# Plugin Engine

## Portfolio Summary

**Plugin Engine** is Moonrise Studios' modular toolkit for building modern Paper plugins. It packages the practical systems that Minecraft projects need most - commands, schedulers, GUIs, messaging, configuration, jobs, and utility helpers - into a reusable engineering layer that speeds up delivery without sacrificing code quality.

Built to work alongside Moss, Plugin Engine gives teams a higher-level application layer on top of the dependency injection and lifecycle foundation, turning repeated plugin patterns into maintained shared infrastructure.

## Why It Exists

Paper plugins regularly need the same building blocks: command registration, task scheduling, inventory UIs, localization, configuration storage, and utility code. Rebuilding those systems from scratch across every project wastes time and introduces inconsistency.

Plugin Engine centralizes that work into a reusable package so teams can focus on gameplay, server features, and product-specific logic instead of framework plumbing.

## What Makes It Valuable

### Shared application toolkit

Plugin Engine converts common plugin systems into reusable modules, reducing duplicate implementation work across projects.

### Better developer velocity

Teams can start from a mature foundation for commands, schedulers, menus, and configuration rather than rebuilding those layers every time.

### Strong Paper-focused ergonomics

The library is tuned for Paper plugin development and includes practical abstractions for entity-safe scheduling, GUI interactions, and plugin bootstrapping.

### Works with Moonrise infrastructure

Because it integrates with Moss and Moonrise's package ecosystem, Plugin Engine fits naturally into a shared studio toolchain.

## Technical Highlights

- Java 21
- Paper-focused module plus shared common module
- Command abstractions built around Incendo Cloud
- Scheduler APIs for sync, async, entity, and location-safe work
- GUI framework for chest, hopper, and paginated interfaces
- MiniMessage, localization, placeholder, and config helpers
- Utility layer for numbers, durations, and common plugin tasks

## Ideal Use Cases

- Paper plugins that need a strong starting framework
- Studio teams maintaining multiple server-side products
- Projects with custom menus, scheduled jobs, player messaging, and configuration-heavy workflows
- Engineering teams that want consistent tooling across plugin codebases

## Portfolio Positioning

Plugin Engine showcases Moonrise Studios' approach to reusable product engineering: identify recurring development patterns, standardize them, and turn them into shared systems that raise quality and reduce build time across the studio.

## Links

- **Source:** https://github.com/moonrise-studios/plugin-engine
- **Organization:** https://github.com/moonrise-studios
- **Packages:** https://repo.moonrise.gg
