# WBS 2.2.10 — BM25検索実装 完了基準チェックリスト

**タスクID**: WBS 2.2.10
**成果物**: BM25SearchService.java + SearchResult.java + テスト
**担当**: ashigaru8 (subtask_056)

## 完了基準

- [ ] BM25SearchService.java が実装されている
  - [ ] search(String query, SearchFilters filters, int topK) → List<SearchResult>
  - [ ] PostgreSQL FTS使用: to_tsvector / to_tsquery
  - [ ] ts_rank_cd() でスコア計算
  - [ ] メタデータフィルタリング（WHERE句動的構築）
- [ ] SearchResult.java（DTO）が実装されている
  - [ ] id, content, score, metadata, sourceUrl フィールド
- [ ] SearchFilters.java が実装されている
- [ ] SearchMode.java が実装されている
- [ ] テストクラスが作成されている
  - [ ] モックリポジトリを使ったユニットテスト
  - [ ] @Tag("integration") の統合テストマーカー
- [ ] Javadocが全て日本語で記述されている
- [ ] ./gradlew build が通る
