# UX Parity Workflow

## Purpose

This workflow turns the old 1.10.2 source into a behavioral specification suitable for a modern 1.20.1 implementation.

## Parity contract template

For every meaningful legacy feature, write:

### Trigger

What causes the feature to start?

### Preconditions

Owner state, equipment, target, health, distance, GUI state, config, etc.

### Player-visible sequence

What does the player see/hear/experience, in order?

### Timing

Ticks, cooldowns, delays, repeated intervals, reaction speed.

### Spatial behavior

Range, follow distance, teleport threshold, pathing, vertical movement.

### State transitions

What starts/stops/changes the mode?

### Interactions

Other AI, owner commands, GUIs, inventory, network packets, sounds, particles.

### End condition

What makes it stop or reset?

### Quirks

Stable player-visible oddities that may be compatibility-relevant.

## Compare current Reforge

Use:

| Aspect | Legacy | Current | Desired |
|---|---|---|---|
| Trigger | | | |
| Timing | | | |
| Target selection | | | |
| Movement | | | |
| Feedback | | | |
| End/reset | | | |

## Status vocabulary

Use one of:

- `NOT_AUDITED`
- `LEGACY_ANALYZED`
- `CURRENT_ANALYZED`
- `GAP_DEFINED`
- `IN_PROGRESS`
- `PARITY_CANDIDATE`
- `AUTOMATED_VERIFIED`
- `MANUAL_VERIFIED`
- `PARITY_CONFIRMED`
- `INTENTIONAL_DIFFERENCE`
- `BLOCKED`

`PARITY_CONFIRMED` should normally require both deterministic validation and the relevant manual/in-game observation when the behavior is perceptual.

## Evidence principle

Do not mark a feature complete because:

- class names match
- methods were ported
- compiler succeeds
- a Goal starts
- a GUI opens

Mark it based on observable contract fulfillment.
