# Codex Task Request Template

以下を Codex に渡す。分からない欄は空欄でもよい。リポジトリから解決できる情報について、Codexは不要な確認質問をせず調査すること。

---

## Task

<実装・修正してほしい内容>

## Expected player experience

<プレイヤーから見て、どう動いてほしいか。分からなければ「temp_1_10_2と同等」と書く>

## Legacy reference

`temp_1_10_2` に同機能がある場合:

<分かるクラス名・機能名。分からなければCodexが調査>

## Current problem

<現在どう違う／壊れているか>

## Constraints

- Minecraft 1.20.1
- Forge 47.4.0
- Parchment mappings
- UX parity is more important than preserving current architecture
- `temp_1_10_2` must remain read-only
- unrelated user changes must not be destroyed

## Architecture permission

必要なら現行設計を破壊・再設計してよい。

ただし、変更はUXパリティ達成に必要な理由を説明すること。

## Validation

Minimum:

```text
compileJava
```

If relevant:

- GameTest
- manual in-game parity scenario
- dedicated-server check
- multiplayer/network check

## Required agent workflow

For a non-trivial legacy feature:

1. `legacy_ux_analyst` extracts the legacy UX contract
2. `reforge_mapper` maps current behavior
3. `forge_api_researcher` verifies uncertain 1.20.1 APIs
4. GPT-5.6 Sol defines the parity gap and architecture
5. `parity_test_designer` designs checks when useful
6. `implementation_worker` implements Sol-approved bounded work
7. `build_verifier` compiles/tests
8. `parity_reviewer` independently compares legacy vs new behavior
9. Sol fixes confirmed issues and reports final UX status

Do not stop at a plan if implementation was requested.
