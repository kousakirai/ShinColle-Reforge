# Entity AI and navigation guide

## Why this needs special treatment

Minecraft Entity AI bugs are often scheduling or state-ownership bugs rather than errors inside the visibly failing Goal.

Always inspect the surrounding system.

## Goal checklist

For a Goal that does not start, starts constantly, or blocks another Goal, inspect:

1. registration in `goalSelector` or `targetSelector`
2. priority
3. Goal flags
4. competing Goals
5. `canUse()`
6. `canContinueToUse()`
7. `start()`
8. `tick()`
9. `stop()`
10. target state
11. navigation state
12. cooldown/tick gating
13. entity AI lifecycle

## Goal flags

Treat flags as scheduling resources.

Common flags include behavior such as:

- movement
- looking
- jumping
- target selection

A high-priority Goal that remains usable or running can prevent lower-priority Goals from ever executing.

When `canUse()` appears never to execute for another Goal, inspect selector scheduling and competing running Goals before assuming registration failed.

## Tick gates

Be precise with modulo checks.

For logic such as:

```java
if (tickCount % N != 0) {
    return false;
}
```

confirm:

- the tick source increments as expected
- the Goal is actually asked to evaluate on those ticks
- another scheduler condition is not preventing evaluation
- the intended cadence is correct

Do not infer the cause from a few log samples alone.

## Targets

Distinguish:

```text
Mob target
custom attack target
owner
owner target
navigation destination
temporary Goal target
```

When wrapping standard target state, keep wrappers thin and consistent.

Avoid maintaining two authoritative combat targets unless the design truly requires both.

## Navigation

Preferred conceptual chain:

```text
Goal
  ↓
PathNavigation
  ↓
MoveControl
  ↓
Entity movement
```

Relevant modern classes may include:

- `PathNavigation`
- `GroundPathNavigation`
- `FlyingPathNavigation`
- `MoveControl`

Do not create a custom navigation abstraction unless project behavior requires it.

## Legacy navigation migration

Do not map old method names one-to-one.

For every legacy call:

1. describe the behavior the old method represented
2. inspect modern navigation state/methods
3. choose the modern operation that has the same semantics
4. verify stop/completion/repath behavior

Examples such as “old no-path check” or “clear current path” must be verified against the actual 1.20.1 class used by the entity.

## Flying entities

For flying entities inspect:

- navigation implementation
- path type assumptions
- MoveControl implementation
- desired movement speed
- vertical movement
- collision/no-clip assumptions
- water/floating behavior
- owner-follow teleport conditions

Do not combine ground-navigation assumptions with flying MoveControl without checking the behavior.

## Attack Goals

For melee/ranged/carrier/aircraft Goals, inspect:

- target acquisition
- range calculation
- line of sight if relevant
- cooldown
- navigation-to-target
- stopping conditions
- target death/removal
- ownership/faction checks
- Goal flag conflicts

## Debug logging

Useful temporary logging should show state transitions, not flood every tick.

Examples of useful state:

```text
goal name
canUse result
target identity/state
navigation done/running
distance
cooldown
currently running competing goals
```

Remove noisy diagnostics once the issue is understood.
