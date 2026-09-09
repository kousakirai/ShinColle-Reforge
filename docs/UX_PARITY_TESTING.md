# UX Parity Testing

## Two-layer validation

### Layer 1 — deterministic

Use GameTests or other automated checks for:

- state transitions
- target selection
- inventory changes
- cooldown/timer boundaries
- health/damage outcomes
- owner/team conditions
- spawn/despawn conditions
- saved-state round trips
- packet-driven server state when testable

### Layer 2 — perceptual/manual

Use in-game scenarios for:

- movement feel
- pathing around terrain
- follow responsiveness
- turn rate
- vertical flight/swim behavior
- GUI layout and usability
- sound timing
- particle feedback
- animation/model feedback
- combat pacing

## Scenario format

```text
Feature:
Legacy source:
Setup:
Initial state:
Player action:
Expected sequence:
Timing/range tolerances:
Expected audiovisual feedback:
Failure signs:
Current result:
Status:
```

## Timing

Do not require exact tick-for-tick equality when modern engine changes make that inappropriate.

Instead define a tolerance based on whether the player can perceive a meaningful difference.

For gameplay-critical mechanics such as cooldowns/ranges, use exact or tight deterministic checks where possible.

## Regression tests

When a parity bug is fixed, prefer adding a regression test for the deterministic portion of that bug.

Do not write tests that assert internal class names or architecture when the behavior can be asserted instead.

## AI parity

For AI, test:

- can it enter the behavior?
- when does it enter?
- what state is authoritative?
- what competing behavior blocks it?
- when does it stop?
- what happens after stop?
- does navigation produce the intended destination?
- is the attack/action cadence correct?

Then manually check movement/combat feel if relevant.
