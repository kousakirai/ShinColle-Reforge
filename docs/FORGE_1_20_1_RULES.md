# Forge 1.20.1 Rules for ShinColle

## Exact environment

Use the repository configuration:

- Minecraft 1.20.1
- Forge 47.4.0
- ForgeGradle 6.x
- Parchment 2023.09.03-1.20.1
- Java 17

## Mapping discipline

Legacy 1.10.2 uses MCP stable_29 names.

Current code uses Parchment over the modern Minecraft/Forge environment.

Never paste old MCP names into current code without verified translation.

Do not mix Yarn names.

## Registration

Current project uses modern Forge registration, including `DeferredRegister` / `RegistryObject`.

Preserve registry IDs when possible because they affect worlds and player experience.

If parity work genuinely requires a registry change, Sol must identify migration implications.

## Lifecycle

Do not assume a removed legacy hook has a one-method modern equivalent.

For entities, inspect:

- construction
- attribute creation
- spawn/finalization
- saved-data read/write
- AI/Brain setup
- server AI tick
- normal tick
- removal/death

## Client/server

Keep dedicated-server safety.

Client rendering/UI code must remain isolated from server/common gameplay logic.

Gameplay authority should be server-side where appropriate.

## Networking

When porting old packets, reproduce the resulting interaction/state behavior, not the old packet structure.

Validate:

- direction
- encode/decode
- execution thread
- server validation
- target/distribution
- state sync timing

## Persistence

For NBT/capabilities/world data:

- preserve meaningful old/current keys when feasible
- define defaults for absent data
- migrate rather than destroy data when practical
- distinguish persistent gameplay state from transient AI state

## Brain/AI

Modern scheduler behavior differs from legacy AI tasks.

Do not infer parity from equal numeric priorities alone.

## Performance

Do not introduce expensive scans/path recalculation every tick just to mimic an old implementation detail.

Reproduce visible behavior efficiently when possible.
