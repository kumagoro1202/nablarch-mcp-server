# WBS 2.1.6 — semantic_search Tool設計 完了基準チェックリスト

**タスクID**: WBS 2.1.6
**成果物**: docs/designs/semantic-search-tool.md
**担当**: ashigaru8 (subtask_056)

## 完了基準

- [ ] 入力スキーマが定義されている
  - [ ] query(string), filters(object?), top_k(int?), mode(string?)
- [ ] 出力スキーマが定義されている
  - [ ] results[{content, score, metadata, source_url}]
- [ ] mode: "hybrid"(default), "vector", "keyword" が設計されている
- [ ] RAGパイプライン呼び出しフローが記述されている
  - [ ] クエリ解析（言語検出、エンティティ抽出）
  - [ ] Embedding生成（Jina v4）
  - [ ] ハイブリッド検索
  - [ ] リランキング
  - [ ] 結果整形
- [ ] MCP Tool登録パターン（@Tool annotation）が設計されている
- [ ] Phase 1 search_api との関係が明記されている
- [ ] hybrid-search.md, reranking.md との整合性が確認されている
