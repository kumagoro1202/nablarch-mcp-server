# WBS 3.2.1 完了基準チェックリスト

> **タスク**: design_handler_queue Tool 実装
> **作成日**: 2026-02-03
> **作成者**: ashigaru1 (subtask_071)

---

## 実装チェック項目

### 1. Tool本体
- [x] DesignHandlerQueueTool.java 作成
- [x] @Tool/@ToolParam アノテーション付与

### 2. handlerqueueサブパッケージ
- [x] HandlerQueueDesigner.java
- [x] HandlerQueueSorter.java
- [x] HandlerQueueXmlGenerator.java
- [x] HandlerQueueValidator.java
- [x] RequirementsMapper.java

### 3. Model DTO
- [x] model/DesignRequest.java
- [x] model/ValidationResult.java

### 4. NablarchKnowledgeBase拡張
- [x] isValidAppType() メソッド追加
- [x] getRequiredHandlers() メソッド追加
- [x] getAllHandlers() メソッド追加
- [x] getAllHandlerConstraints() メソッド追加

### 5. 設定登録
- [x] McpServerConfigへの登録（PR時に調整要）

### 6. 処理フロー
- [x] Phase 1-7 全工程実装済み

---

## 成果物

| ファイル | 説明 |
|---------|------|
| DesignHandlerQueueTool.java | MCP Tool本体 |
| HandlerQueueDesigner.java | オーケストレーター |
| HandlerQueueSorter.java | トポロジカルソート |
| HandlerQueueXmlGenerator.java | XML生成 |
| HandlerQueueValidator.java | 制約検証 |
| RequirementsMapper.java | 要件マッピング |
| DesignRequest.java | リクエストDTO |
| ValidationResult.java | 検証結果DTO |
