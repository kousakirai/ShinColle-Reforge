# Legacy mod porting rules

## Core principle

Port behavior, not syntax.

An old Minecraft/Forge class or method name is evidence of historical structure, not proof of the correct 1.20.1 replacement.

## Migration procedure

For each legacy subsystem:

### 1. Characterize old behavior

Document:

- inputs
- state read
- state written
- side effects
- lifecycle timing
- interaction with other systems

### 2. Identify modern ownership

Determine which 1.20.1 system now owns the behavior.

Examples:

- Goal system
- navigation
- MoveControl
- registration
- event lifecycle
- networking
- persistence
- attributes
- spawning

### 3. Check mapping names

Translate old MCP/Yarn names to the mapping actually used by the project.

### 4. Implement smallest equivalent behavior

Avoid preserving obsolete abstractions solely because they existed in the old source.

Also avoid rewriting the whole subsystem if a compatibility wrapper is the safer intermediate step.

### 5. Validate

Compile first.

Then validate behavior that the compiler cannot prove.

## Common migration traps

### Mechanical method replacement

Wrong approach:

```text
oldMethodName -> similarLookingNewMethodName
```

Correct approach:

```text
old semantic behavior
-> modern subsystem
-> verified modern API
```

### Deprecated hook replacement

A deprecated or removed lifecycle hook may have been split across multiple modern hooks.

Inspect call timing, not only method signature.

### Client/server assumptions

Older code may assume integrated-client behavior that crashes or behaves incorrectly on a dedicated server.

### Registry architecture

Do not preserve obsolete registration timing/patterns.

### AI scheduler behavior

Old AI task scheduling semantics may differ from modern GoalSelector behavior.

### Data compatibility

Do not casually rename NBT keys, registry IDs, packet contracts, or save identifiers.

## Recommended porting report

For each migrated area, record:

```text
Legacy behavior:
Modern owner:
Modern implementation:
Compatibility notes:
Validation:
Remaining gameplay checks:
```
