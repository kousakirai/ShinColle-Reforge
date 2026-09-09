# ShinColle-Reforge Codex設定（UX最優先版）

このパッケージは、アップロードされた ShinColle-Reforge の構成を前提に作成した Codex 用オーバーレイです。

## 最上位目的

```text
temp_1_10_2 にある旧ShinColle
        ↓
プレイヤー体験・挙動を仕様として抽出
        ↓
Minecraft Forge 1.20.1 で再現
```

現行Reforgeの設計維持や最小diffは最優先ではありません。

必要ならSolの判断でAI・Navigation・Target管理・GUI等を再設計できます。

## エージェント構成

### Main

```text
GPT-5.6 Sol
```

役割:

- UX仕様の最終解釈
- legacy/current差分統合
- アーキテクチャ
- Brain移行判断
- 作業分割
- 統合
- 最終レビュー

### Subagents

全て:

```text
GPT-5.6 Luna
reasoning = max
```

用意している役割:

```text
legacy_ux_analyst
reforge_mapper
forge_api_researcher
implementation_worker
build_verifier
parity_reviewer
parity_test_designer
```

## 導入

このZIPの**中身**を ShinColle-Reforge のGitルートへコピーする。

最終的には:

```text
ShinColle-Reforge/
├─ AGENTS.md
├─ .codex/
│  ├─ config.toml
│  └─ agents/
│     ├─ legacy_ux_analyst.toml
│     ├─ reforge_mapper.toml
│     ├─ forge_api_researcher.toml
│     ├─ implementation_worker.toml
│     ├─ build_verifier.toml
│     ├─ parity_reviewer.toml
│     └─ parity_test_designer.toml
├─ docs/
│  ├─ PRIMARY_DIRECTIVE.md
│  ├─ PROJECT_CONTEXT.md
│  ├─ CODEX_WORKFLOW.md
│  ├─ UX_PARITY_WORKFLOW.md
│  ├─ UX_PARITY_MATRIX.md
│  ├─ LEGACY_SOURCE_GUIDE.md
│  ├─ LEGACY_CURRENT_AI_MAP.md
│  ├─ AI_BRAIN_MIGRATION.md
│  ├─ FORGE_1_20_1_RULES.md
│  ├─ LEGACY_PORTING_RULES.md
│  ├─ BUILD_AND_VALIDATION.md
│  ├─ UX_PARITY_TESTING.md
│  ├─ TASK_REQUEST_TEMPLATE.md
│  └─ DECISION_RECORD_TEMPLATE.md
├─ FIRST_PROMPT.txt
├─ temp_1_10_2/
└─ src/
```

## 重要

Codexは通常、新しいセッション開始時に `AGENTS.md` の指示チェーンを構築する。

ファイルをコピーした後は新規Codexセッションを開始すること。

また、プロジェクトがCodex側で未信頼扱いの場合、プロジェクトローカル `.codex/` 設定が利用されないことがあるため、利用環境でリポジトリが信頼済みになっていることを確認する。

## 最初の起動

`FIRST_PROMPT.txt` を最初の依頼のベースに使用する。

## Luna Maxについて

設定では各カスタムサブエージェントに:

```toml
model = "gpt-5.6-luna"
model_reasoning_effort = "max"
```

を明示している。

利用中のCodex環境が Luna で `max` を公開していない場合は、Codexのモデル一覧でその環境が実際にサポートしている推論強度を確認すること。

希望構成を勝手に別モデルへ置き換えない。

## Solのreasoning effort

メインモデルは `gpt-5.6-sol` に固定しているが、Sol側の推論強度はファイルでは固定していない。

タスクに応じてCodex側で選択できる。

複雑なAI/Brain/Navigation再設計では高い推論強度を推奨する。

## UXパリティ管理

`docs/UX_PARITY_MATRIX.md` を開発進捗の中心資料として使う。

「port済み」ではなく:

- 旧版UXを調査したか
- 現行との差分を特定したか
- 自動テストしたか
- 実ゲームで確認したか

を管理する。
