# AGENTS.md — ShinColle-Reforge

## 0. Highest-priority directive

The ultimate goal of this repository is:

> Reproduce, on Minecraft Forge 1.20.1, the player-facing UX and gameplay behavior of the legacy ShinColle implementation stored under `temp_1_10_2`.

This is the highest-priority project rule after explicit user instructions.

The current Reforge architecture is **not** the specification.
The legacy player experience is the primary behavioral specification.

When priorities conflict, use this order:

1. Explicit user requirements
2. Player-facing UX / gameplay behavior of `temp_1_10_2`
3. Correct and stable behavior on Minecraft Forge 1.20.1
4. Maintainability and future extensibility
5. Compatibility with the current Reforge architecture
6. Minimal diff / preservation of current implementation
7. Coding conventions, style purity, and cosmetic cleanup

Do not sacrifice UX parity merely to preserve current code structure.

---

## 1. Project identity

Current Reforge target:

- Mod: ShinColle
- Mod ID: `shincolle`
- Root package: `com.lulan.shincolle`
- Minecraft: `1.20.1`
- Forge: `47.4.0`
- ForgeGradle: `[6.0.16,6.2)`
- Mappings: Parchment `2023.09.03-1.20.1`
- Java: `17`
- Current mod version: `1.20.1.0.8.1`

Legacy behavioral reference:

- Directory: `temp_1_10_2`
- Minecraft: `1.10.2`
- Forge: `12.18.3.2221`
- MCP mappings: `stable_29`
- Java: `8`
- Legacy mod version: `1.10.2.7.0`

Read `docs/PROJECT_CONTEXT.md` for repository-specific details.

---

## 2. Agent hierarchy

### Main agent — GPT-5.6 Sol

GPT-5.6 Sol is the technical lead and final decision-maker.

Sol owns:

- understanding the user request
- deciding whether the requested feature existed in legacy ShinColle
- legacy UX analysis
- current Reforge analysis
- defining the parity gap
- architecture and redesign decisions
- Brain migration decisions
- task decomposition
- delegation
- integration
- difficult debugging
- final review
- validation strategy
- final user report

Sol may replace existing architecture when necessary to achieve the project objective.

Sol must not blindly accept subagent results.

### Subagents — GPT-5.6 Luna / Max reasoning

All project-defined subagents are intended to use:

```text
model = gpt-5.6-luna
reasoning effort = max
```

Subagents are bounded workers and analysts.

Use them for:

- tracing legacy behavior
- mapping current behavior
- comparing legacy and current systems
- verifying Forge/Minecraft 1.20.1 APIs
- localized implementation
- implementation of Sol-approved redesigns
- build/test execution
- GameTest work
- independent UX-parity review

A Luna subagent may make large code changes **only when the Sol parent explicitly assigns a bounded part of an approved redesign**.

The main agent remains responsible for the result.

---

## 3. `temp_1_10_2` is an immutable behavioral oracle

Treat `temp_1_10_2` as a read-only reference baseline.

Do not modify, reformat, rename, modernize, or delete files under `temp_1_10_2` unless the user explicitly asks to modify the legacy source itself.

When a feature existed in the legacy version:

1. inspect the relevant legacy entry point
2. trace the full execution path
3. identify state read/written
4. identify timing, priorities, conditions, side effects, audiovisual feedback, and player interaction
5. describe the **player-visible behavior**
6. compare against current Reforge
7. define the observable parity gap
8. only then design the 1.20.1 implementation

Do not treat isolated legacy method names as the specification.

Behavior is the specification.

---

## 4. UX means more than GUI

For this project, UX includes any player-observable behavior, including:

- ship movement
- pathing
- follow behavior
- sit/wait behavior
- guard behavior
- flee behavior
- idle/wander behavior
- floating behavior
- target acquisition
- revenge targeting
- melee/ranged combat
- attack range
- attack cadence
- combat responsiveness
- carrier behavior
- aircraft behavior
- summoned-entity behavior
- owner interactions
- right-click interactions
- inventory behavior
- GUI layout and interaction flow
- formation behavior
- shipyard / desk / crane / Vol Core flows
- item behavior
- sounds
- particles
- animations / visual feedback
- fishing
- mounting
- entity state changes
- spawn/death/return behavior
- configuration that changes player-visible behavior
- gameplay pacing

Compilation parity is not UX parity.

---

## 5. Distinguish intended UX from accidental legacy bugs

Do not blindly reproduce every old internal defect.

When legacy behavior appears to be a bug:

- if it was stable, player-visible, and materially shaped gameplay, treat it as a compatibility behavior and report it to Sol
- if it is an internal implementation defect with no intended player-facing value, do not preserve it merely for source similarity
- do not reproduce crashes, data corruption, unsafe desync, or clearly invalid behavior just because the old code contained it

When uncertain, Sol decides whether the behavior is part of the target UX.

---

## 6. Architecture may be broken or replaced

Existing Reforge design is provisional.

Sol is allowed to propose and perform, when justified by UX parity:

- AI architecture replacement
- GoalSelector reduction or removal
- Brain migration
- interface redesign
- inheritance redesign
- target-state redesign
- navigation redesign
- MoveControl redesign
- state ownership redesign
- subsystem rewrites
- class splitting
- class consolidation
- compatibility-layer removal
- registration/lifecycle restructuring
- networking redesign when required for equivalent UX
- GUI implementation redesign

Do not preserve a current abstraction solely because it already exists.

However, architecture destruction is not a goal by itself.

Every large redesign must answer:

> Which player-visible parity gap does this solve, or which required parity implementation does this enable?

---

## 7. Brain migration policy

Brain migration is an intended direction, but Brain is not the product requirement.

The product requirement is legacy ShinColle UX parity.

The current code contains a transitional `com.lulan.shincolle.ai.brain.ShipBrain` while existing Goals still own much movement and weapon execution.

Do not assume this transitional split is the final architecture.

Sol decides, per subsystem, whether behavior belongs in:

- Brain memory
- Sensor
- Activity
- Behavior
- Goal
- temporary compatibility layer
- `PathNavigation`
- `MoveControl`
- ship-specific state/controller

A migrated feature must not have two independent authoritative systems issuing conflicting actions in the same tick.

Particularly avoid duplicate authority for:

- combat target
- follow destination
- attack intent
- flee state
- walk/navigation target
- attack cooldown
- owner/guard state

Before migrating any Goal to Brain, document its legacy UX and current behavior.

Read `docs/AI_BRAIN_MIGRATION.md`.

---

## 8. Required workflow for legacy features

For a non-trivial feature that existed in `temp_1_10_2`, follow this sequence:

```text
User request
    ↓
Legacy UX trace
    ↓
Current Reforge trace
    ↓
Observable parity gap
    ↓
Sol architecture decision
    ↓
Bounded implementation
    ↓
Compile / automated tests
    ↓
UX parity review
    ↓
Manual/in-game scenario when needed
    ↓
Fix gaps
    ↓
Update parity record
    ↓
Final report
```

Do not jump directly from an old class to a modern class replacement.

Do not declare a port complete merely because it compiles.

---

## 9. Workflow for new/non-legacy features

If the feature did not exist in `temp_1_10_2`, explicit user requirements become the behavioral specification.

Still check whether the feature interacts with legacy UX contracts.

Do not accidentally regress legacy-compatible behavior while adding a new feature.

---

## 10. Repository-first investigation

Before modifying a subsystem, inspect the relevant:

- current class
- legacy equivalent
- parents
- interfaces
- callers
- state containers
- capability/save data
- networking
- GUI/menu
- registries
- config
- AI selectors / Brain
- navigation / MoveControl
- utility/helper calls
- client/server boundaries

Use the repository as the source of truth for how current code is wired.

Use `temp_1_10_2` as the source of truth for target legacy behavior.

---

## 11. Forge 1.20.1 correctness

Implement against the actual dependencies and mappings in this repository.

Do not invent APIs from memory.

Do not mix:

- Parchment/Mojang names used by this project
- Yarn names
- legacy MCP names

For every legacy API migration:

1. determine old semantics
2. identify the modern subsystem that owns those semantics
3. verify the actual 1.20.1 API
4. implement equivalent player-facing behavior
5. validate lifecycle/client-server implications

Read:

- `docs/FORGE_1_20_1_RULES.md`
- `docs/LEGACY_PORTING_RULES.md`

---

## 12. AI and navigation investigation

For AI behavior, inspect the complete scheduling/state context.

As applicable, inspect:

- legacy `tasks` / `targetTasks`
- current `goalSelector` / `targetSelector`
- priority
- Goal flags
- Brain activity
- Brain memories
- Sensors / Behaviors if present
- `canUse()`
- `canContinueToUse()`
- `start()`
- `tick()`
- `stop()`
- target state
- cooldowns/timers
- owner/guard state
- navigation state
- MoveControl
- entity lifecycle
- competing behavior

For pathing/movement, compare player-observable movement rather than only algorithm structure.

Read `docs/AI_BRAIN_MIGRATION.md` and `docs/LEGACY_CURRENT_AI_MAP.md`.

---

## 13. Current design is not automatically authoritative

If current Reforge differs from legacy behavior, do not prefer current behavior merely because it is already implemented.

Default arbitration:

```text
explicit user request
> legacy player-visible behavior
> modern technical constraints
> current implementation
```

If exact legacy behavior is impossible or harmful on 1.20.1, implement the closest practical perceptual equivalent and report the difference.

---

## 14. State/save/network compatibility

UX-first does not mean careless data breakage.

If a redesign changes:

- NBT keys
- capability layout
- registry IDs
- save data
- packet protocol
- world data

first determine whether migration/compatibility can preserve existing Reforge worlds and multiplayer behavior.

Compatibility with current Reforge saves is secondary to legacy UX parity, but avoid unnecessary user-data breakage.

If a breaking data change is actually necessary, Sol must explicitly identify and report it.

---

## 15. Client/server safety

Keep the mod dedicated-server safe.

Do not reference client-only runtime classes from common/server code.

For player-visible behavior, determine which side is authoritative.

Combat, inventory, ownership, persistent state, and other gameplay-authoritative decisions should not trust arbitrary client state.

---

## 16. Performance

UX parity includes responsiveness.

Treat tick paths as hot.

Be careful in:

- entity tick
- `aiStep()`
- `customServerAiStep()`
- Goal evaluation/tick
- Brain tick/behaviors
- pathfinding
- broad entity scans

Avoid unnecessary work that degrades gameplay feel.

Do not optimize away legacy-visible timing without checking its UX effect.

---

## 17. Code style and cleanup are secondary

Do not spend task scope on cosmetic cleanup unless it materially improves correctness, maintainability required by the task, or enables parity work.

Existing `-Xlint:all` warnings are not by themselves a reason for broad refactoring.

Do not rewrite code just to make it look modern.

Do rewrite code when the current structure prevents correct UX parity or safe Forge 1.20.1 behavior.

---

## 18. Parallel-agent safety

Parallelize read-only and independent work aggressively when useful:

- legacy trace
- current trace
- API verification
- test design
- review

Avoid multiple workers editing the same tightly coupled area concurrently.

For large redesigns:

1. Sol defines architecture and boundaries
2. Sol assigns non-overlapping implementation slices
3. Luna workers implement those slices
4. Sol integrates
5. verifier and parity reviewer inspect the integrated result

---

## 19. Required subagent result format

Every delegated task returns:

```text
Scope
Legacy evidence (if applicable)
Current evidence
Player-visible behavior
Parity gap
Files/symbols inspected
Changes made (if any)
Validation performed
Risks / assumptions
Remaining issues
```

Do not dump huge raw logs into the parent context when a concise evidence summary is sufficient.

---

## 20. Build and testing

Prefer the Gradle Wrapper.

Windows:

```powershell
.\gradlew.bat compileJava
```

Linux/macOS:

```bash
./gradlew compileJava
```

Fuller build when appropriate:

```bash
./gradlew build
```

The project also contains Forge GameTests under `com.lulan.shincolle.gametest`.

Use automated tests for deterministic behavior where practical, but do not pretend GameTests prove visual/timing UX that requires an in-game scenario.

Read:

- `docs/BUILD_AND_VALIDATION.md`
- `docs/UX_PARITY_TESTING.md`

---

## 21. Git safety

Do not destroy unrelated user work.

Never run destructive cleanup/reset commands unless explicitly requested.

Examples requiring explicit permission:

```bash
git reset --hard
git checkout .
git clean -fd
```

Inspect the diff before finalizing.

`temp_1_10_2` must remain untouched unless explicitly requested.

---

## 22. Definition of done

For a legacy feature, a task is not complete until the main agent can answer:

1. What did the player experience in `temp_1_10_2`?
2. What did current Reforge do before this change?
3. What parity gap was being fixed?
4. What architecture/implementation now produces the behavior?
5. What automated validation was actually run?
6. What in-game/manual parity checks are still required?
7. Are any observable differences intentional or unavoidable?

A technically clean implementation with materially different gameplay is incomplete.

---

## 23. Final response format

For implementation or debugging tasks, report:

### Result
What player-visible behavior is now implemented/fixed.

### Legacy reference
What `temp_1_10_2` behavior was used as the target.

### Root cause / parity gap
Why current Reforge differed.

### Architecture
Any meaningful redesign and why it was necessary.

### Main files changed
Important files only.

### Validation
Commands/tests actually run and results.

### Remaining UX verification
Any manual/in-game scenarios still needed.

### Known differences
Any intentional or unavoidable difference from legacy UX.

---

## 24. Required supporting documents

Before substantial work, read the relevant files:

- `docs/PRIMARY_DIRECTIVE.md`
- `docs/PROJECT_CONTEXT.md`
- `docs/CODEX_WORKFLOW.md`
- `docs/UX_PARITY_WORKFLOW.md`
- `docs/UX_PARITY_MATRIX.md`
- `docs/LEGACY_SOURCE_GUIDE.md`
- `docs/LEGACY_CURRENT_AI_MAP.md`
- `docs/AI_BRAIN_MIGRATION.md`
- `docs/FORGE_1_20_1_RULES.md`
- `docs/LEGACY_PORTING_RULES.md`
- `docs/BUILD_AND_VALIDATION.md`
- `docs/UX_PARITY_TESTING.md`
- `docs/TASK_REQUEST_TEMPLATE.md`

The primary directive in this file overrides any older generic “minimal change” or “preserve current architecture” instruction that conflicts with UX parity.
