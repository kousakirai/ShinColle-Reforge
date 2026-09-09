# Legacy Source Guide

## Rule

`temp_1_10_2` is the behavioral baseline and must remain read-only.

## How to analyze a legacy feature

Do not read only the obvious class.

For AI:

```text
EntityAI...
→ BasicEntityShip task registration
→ interfaces/state arrays
→ TargetHelper / EntityHelper / CombatHelper
→ navigation / MoveHelper
→ config / ID timers
→ packets / sounds / particles
```

For GUI:

```text
GUI
→ Container
→ Slot behavior
→ TileEntity / Entity state
→ packet handlers
→ inventory capability
→ item/block interaction entry point
```

For entity interaction:

```text
item/right-click/input
→ entity interaction method/helper
→ capability/state
→ packet
→ audiovisual feedback
```

## Extract semantics, not syntax

Legacy 1.10.2 uses old APIs such as:

- `EntityAIBase`
- `tasks`
- `targetTasks`
- old navigation/move-helper classes
- Forge 1.10.2 registration/events/network patterns

These are not direct implementation requirements.

The requirement is the observable behavior they produced.

## Legacy bugs

Label suspicious behavior as:

```text
PLAYER_VISIBLE_QUIRK
INTERNAL_BUG
CRASH/DATA_RISK
UNCERTAIN
```

Do not silently “fix” a stable player-visible quirk during parity work without noting it.

Do not intentionally reproduce crash/data-corruption behavior.

## Asset/reference comparison

For visual/audio UX, inspect legacy and current resources as well as Java behavior:

- textures
- GUI textures
- models
- sounds
- language strings
- particles/effects
- item/block assets

The visual implementation technology may change; the target experience should remain recognizable.
