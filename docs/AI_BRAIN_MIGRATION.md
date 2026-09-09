# AI / Brain Migration Directive

## Purpose

Move toward a modern Brain-centered architecture when it improves clarity, control, and parity—but never at the expense of legacy player experience.

## Current state

Current `ShipBrain` is transitional.

It:

- provides several vanilla memories
- registers CORE/IDLE/FIGHT/PANIC activities
- currently has no custom ship sensors
- mirrors current target/hurt/flee state
- does not yet own most movement/weapon execution

Current Goals still perform much of the actual AI.

This is not a permanent constraint.

## Target principle

The eventual architecture should have clear state ownership and avoid two competing schedulers commanding the same behavior.

A possible conceptual structure is:

```text
Ship Entity
    ↓
Brain / ship state
    ├─ perception / sensors
    ├─ memories
    ├─ activity / intent
    └─ behaviors
            ↓
      navigation / movement
            ↓
      PathNavigation
            ↓
      MoveControl
```

This is a conceptual target, not a mandatory class diagram.

## Migration rule

Before migrating a legacy Goal:

1. extract legacy UX contract
2. document current Goal behavior
3. identify scheduler dependencies and Goal flags
4. identify target/navigation/timer state ownership
5. decide the new authoritative state
6. implement Brain pieces or another architecture
7. prevent the old and new systems from both commanding the same action
8. test parity
9. remove obsolete legacy/current compatibility logic only after replacement is proven

## State authority

Every important state should have one clear authority.

Examples:

### Combat target

Potential representations:

- `Mob#getTarget()`
- custom `atkTarget` / `aiTarget`
- `MemoryModuleType.ATTACK_TARGET`

Do not let all three drift independently.

During transition, synchronization adapters are acceptable, but explicitly document which representation is authoritative.

### Movement intent

Do not allow a running Goal and Brain Behavior to independently set conflicting walk/navigation targets in the same tick.

### Flee/activity state

If Brain PANIC becomes authoritative, old flee scheduling must be retired or made subordinate.

## Migration order

Do not hard-code a universal migration order before auditing behavior.

Sol should select slices based on:

- parity gaps
- coupling
- state ownership
- testability
- risk

A good slice is a behavior whose trigger, state, action, and completion can be defined and verified independently.

## Brain is optional per behavior

If a legacy behavior maps poorly to vanilla Brain abstractions and a small custom controller reproduces the UX more reliably, Sol may choose that design.

The criterion is:

```text
behavioral fidelity + correctness + maintainability
```

not “maximum Brain usage.”

## Acceptance criteria for migrated AI

A migrated behavior must specify:

- legacy trigger
- current/new trigger
- priority/competition semantics
- target semantics
- movement semantics
- timing/cooldown
- stop/reset behavior
- owner/team behavior
- automated checks
- manual parity scenario
