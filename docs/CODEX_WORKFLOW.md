# Codex Workflow — UX-first ShinColle port

## Standard task classification

Sol first classifies the task:

### A. Legacy parity task

The feature existed in `temp_1_10_2`.

Use the full parity workflow.

### B. New feature

No meaningful legacy equivalent exists.

Use explicit user requirements as the UX specification and perform regression checks against related legacy behavior.

### C. Infrastructure-only task

Build tooling, datagen, internal test harness, etc.

Prioritize correctness and avoid unnecessary gameplay changes.

## Full parity workflow

### 1. Legacy analysis

Delegate to `legacy_ux_analyst` for non-trivial work.

Output must be a player-visible contract, not just a class summary.

### 2. Current mapping

Delegate to `reforge_mapper`.

Determine current behavior and state ownership.

### 3. Gap definition

Sol synthesizes a parity gap:

```text
Legacy expected:
Current actual:
Observable difference:
Why it matters to UX:
```

### 4. API verification

Use `forge_api_researcher` for any uncertain 1.20.1/Parchment/Forge behavior.

### 5. Architecture decision

Sol chooses the design that best serves parity.

Possible result:

- localized fix
- Goal repair
- Brain behavior migration
- state ownership cleanup
- navigation rewrite
- GUI/network rewrite
- subsystem replacement

Do not choose minimal diff by default.

Choose the smallest architecture that reliably reproduces the required behavior.

### 6. Test design

For consequential behavior, ask `parity_test_designer` to separate:

- deterministic automated checks
- manual/in-game checks

### 7. Implementation

Delegate bounded slices to `implementation_worker` when useful.

For a large redesign, define non-overlapping file/system ownership first.

### 8. Build verification

Use `build_verifier`.

At minimum, compile affected Java when feasible.

### 9. Independent parity review

Use `parity_reviewer` for:

- AI
- navigation
- combat
- GUI flow
- networking
- save/state changes
- large redesigns

### 10. Repair loop

Sol fixes confirmed issues and reruns the relevant checks.

### 11. Parity record

Update `docs/UX_PARITY_MATRIX.md` when the task materially changes/audits a tracked feature.

### 12. Final report

Explain behavior, not merely code.

## Parallelization pattern

Good initial parallel work:

```text
legacy_ux_analyst  ─┐
reforge_mapper      ├─> Sol gap synthesis
api_researcher      ┘
```

Then:

```text
Sol architecture
      ↓
implementation worker(s)
      ↓
build verifier + parity reviewer
      ↓
Sol integration/fix
```

Do not have multiple workers independently redesign the same AI subsystem.

## Escalation triggers

A Luna worker returns to Sol rather than expanding scope when it discovers:

- change to persistent format
- registry ID change
- network compatibility break
- shared entity hierarchy redesign beyond assigned boundary
- ambiguity about intended legacy UX
- a conflict between exact legacy behavior and modern safety/stability
- a second subsystem that must be redesigned to complete the task

Sol then decides whether to widen the task.
