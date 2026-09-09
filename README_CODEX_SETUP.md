# Codex setup for Minecraft Forge 1.20.1

This directory is a ready-to-copy Codex instruction kit for a Minecraft Forge 1.20.1 repository.

## Intended model layout

- Main/root agent: `gpt-5.6-sol`
- Subagents: `gpt-5.6-luna`
- Subagent reasoning effort: `max`

The project configuration is in:

```text
.codex/config.toml
```

The dedicated subagents are in:

```text
.codex/agents/
```

## Install

Copy the contents of this kit into the root of your Forge repository.

Expected result:

```text
your-mod/
├─ AGENTS.md
├─ .codex/
│  ├─ config.toml
│  └─ agents/
│     ├─ worker.toml
│     ├─ explorer.toml
│     ├─ reviewer.toml
│     ├─ verifier.toml
│     └─ api_researcher.toml
├─ docs/
│  ├─ CODEX_WORKFLOW.md
│  ├─ FORGE_1_20_1_RULES.md
│  ├─ ENTITY_AI_AND_NAVIGATION.md
│  ├─ PORTING_RULES.md
│  ├─ BUILD_AND_VALIDATION.md
│  ├─ TASK_REQUEST_TEMPLATE.md
│  └─ PROJECT_CONTEXT_TEMPLATE.md
├─ build.gradle
├─ gradle.properties
└─ src/
```

Start a new Codex session after copying the files so the instruction chain is rebuilt.

## What each file does

### `AGENTS.md`

Primary project policy.

It defines:

- Sol as technical lead
- Luna-Max subagent policy
- repository-first investigation
- Forge 1.20.1 constraints
- AI/navigation rules
- client/server safety
- build expectations
- Git safety
- completion criteria

### `.codex/config.toml`

Project-scoped model/orchestration configuration.

It selects Sol for the root session and Luna + Max as the default spawned-agent configuration.

### `.codex/agents/*.toml`

Narrow custom subagents:

- `forge_explorer`: read-only repository investigation
- `forge_worker`: bounded implementation
- `forge_reviewer`: independent review
- `forge_verifier`: build/test execution and failure analysis
- `forge_api_researcher`: version/API/mapping verification

## Recommended first prompt

After opening the repository in Codex:

```text
Read AGENTS.md and the relevant docs first.

Analyze the repository before changing anything.
Have forge_explorer map the relevant code path and, when Forge/Minecraft API uncertainty exists, have forge_api_researcher verify it.
Then have forge_worker implement the smallest correct change.
Have forge_verifier compile/test it and forge_reviewer independently review the result.
Wait for the delegated agents that are relevant, integrate their findings, fix issues, and report the final validation.

Task:
<write the task here>
```

For small edits, the main agent does not need to spawn every role. Use only the agents that materially improve the result.

## Luna Max compatibility note

The kit requests:

```toml
model = "gpt-5.6-luna"
model_reasoning_effort = "max"
```

Reasoning-effort availability can depend on the model/client/account rollout.

If Codex reports that `max` is unsupported for Luna, inspect the model picker or model capability list and use the highest Luna reasoning effort actually exposed by your installation.

Do not silently substitute a different model if the goal is to keep Luna as the worker.

## Main-agent reasoning effort

This kit intentionally fixes the root **model** to `gpt-5.6-sol` but does not force a root reasoning effort.

That lets you choose the appropriate Sol reasoning level interactively:

- normal work: Medium/High
- difficult architecture/debugging: Extra High/Max
- proactive multi-agent orchestration, when available and desired: Ultra

The subagents remain explicitly Luna-Max through the project configuration/custom-agent files.

## Project-specific customization

Fill out:

```text
docs/PROJECT_CONTEXT_TEMPLATE.md
```

with:

- mod id
- package root
- Forge version
- mappings
- Java version
- major systems
- legacy source version
- important invariants
- known broken systems

Once filled, rename it to `docs/PROJECT_CONTEXT.md` and add it to the detailed-guidance list in `AGENTS.md`.

## Important

These files are instructions and configuration, not a substitute for inspecting the real repository.

Codex must still inspect `build.gradle`, `gradle.properties`, mappings, source code, and relevant call paths before making version-sensitive changes.
