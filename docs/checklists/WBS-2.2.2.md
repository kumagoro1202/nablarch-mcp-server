# WBS 2.2.2 ベクトルDBスキーマ実装 完了基準チェックリスト

- [ ] build.gradle.kts に JPA / PostgreSQL / pgvector / Flyway 依存が追加されている
- [ ] spring-boot-starter-webflux が追加されている（Embedding API用）
- [ ] テスト用H2依存が追加されている
- [ ] V1__create_vector_tables.sql が存在する
- [ ] document_chunks テーブル定義がある（vector(1024), メタデータカラム）
- [ ] code_chunks テーブル定義がある（vector(1024), メタデータカラム）
- [ ] IVFFlat ベクトルインデックスが定義されている（lists=100, cosine）
- [ ] FTS (GIN) インデックスが定義されている
- [ ] メタデータ用インデックスが定義されている
