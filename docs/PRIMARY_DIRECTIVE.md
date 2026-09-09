# Primary Directive — Legacy ShinColle UX Parity

## Final objective

The final objective of ShinColle-Reforge is to obtain the same or practically equivalent player experience as the ShinColle 1.10.2 source stored in:

```text
temp_1_10_2/
```

on Minecraft Forge 1.20.1.

This objective is more important than preserving the current Reforge architecture or following a particular coding style.

## What is authoritative?

### Highest authority

Explicit user requirements.

### Behavioral authority

The player-visible behavior of `temp_1_10_2`.

### Technical authority

The actual Minecraft Forge 1.20.1/Parchment APIs and runtime constraints.

### Non-authoritative implementation

The current Reforge architecture is a work-in-progress implementation and may be replaced.

## Architectural freedom

If necessary for parity, the project may:

- remove or replace current Goals
- migrate to Brain
- redesign navigation
- redesign MoveControl
- redesign target/state ownership
- change interfaces and inheritance
- rewrite subsystems
- replace GUI/network internals

A redesign should be justified by behavior, not aesthetics.

## Brain

Brain migration is allowed and intended where useful.

Brain parity is not the objective.
Legacy UX parity is the objective.

A Brain-based implementation is correct only if it preserves or improves fidelity to the target behavior.

## Completion standard

For a legacy feature, “it compiles” means only that the Java/API layer is plausible.

Completion requires a documented legacy behavior contract and evidence that the new implementation matches it closely enough from the player's perspective.
