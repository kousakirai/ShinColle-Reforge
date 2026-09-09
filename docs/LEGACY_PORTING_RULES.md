# Legacy Porting Rules — Behavior-first

## Core rule

Port observable behavior, not source structure.

## Porting sequence

For each legacy subsystem:

1. identify player-visible contract
2. map legacy state/timing/dependencies
3. map current Reforge state
4. define parity gap
5. identify modern 1.20.1 owner/subsystem
6. design modern implementation
7. validate observable equivalence

## Architecture replacement is allowed

Unlike a conservative maintenance port, this project explicitly allows replacing current Reforge architecture when that is the best route to legacy UX parity.

Examples include:

- replacing a broken Goal port with Brain behavior
- replacing custom navigation if it cannot reproduce legacy motion
- replacing current UI/network plumbing while preserving interaction UX
- consolidating duplicate target state

## What not to preserve automatically

Do not preserve merely because it exists:

- old MCP method names
- old inheritance
- old event bus patterns
- old packet structure
- current compatibility wrappers
- current Goal implementation
- current custom navigation implementation

Preserve behavior.

## What deserves extra caution

These can affect user data or content identity:

- registry IDs
- NBT keys
- capability serialization
- world data
- recipe/resource IDs
- packet compatibility in multiplayer

Prefer migration paths.

## Porting report

For each migrated feature record:

```text
Legacy UX:
Legacy source:
Current pre-change behavior:
Parity gap:
Modern design:
Validation:
Known differences:
```
