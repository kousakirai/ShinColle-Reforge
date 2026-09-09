# Project context

Fill this file once for the specific mod repository.

After filling it, rename it to:

```text
PROJECT_CONTEXT.md
```

Then add it to the detailed-guidance section of `AGENTS.md`.

## Identity

Mod name:

Mod ID:

Root Java package:

Current project/repository name:

## Versions

Minecraft:

Forge:

ForgeGradle:

Java:

Mappings:

Gradle:

## Source history

Original Minecraft version, if this is a port:

Original Forge version:

Original mappings, if known:

Location of legacy source:

## Main architecture

Main mod class:

Registry classes:

Entity registry:

Item/block registry:

Network setup:

Config system:

Save/persistence system:

## Entity architecture

Base entity classes:

Important interfaces:

Target ownership model:

Navigation classes:

MoveControl classes:

Main Goal classes:

Owner/faction system:

## Important invariants

List behavior that must not change casually.

Examples:

- existing registry IDs must stay stable
- old worlds must remain loadable
- ship owner behavior must stay compatible
- custom movement must support flying entities
- server is authoritative for combat state

## Known incomplete/broken areas

List systems currently being ported or known to be incomplete.

## Build notes

Standard compile command:

Standard test/build command:

Known environment requirements:

## External reference mods

List any external/open-source mods used as architectural references and what is being learned from them.

Do not treat reference-mod code as drop-in code. Translate concepts to this project's version and architecture.
