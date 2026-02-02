# WBS 2.2.1 pgvector環境構築 完了基準チェックリスト

- [ ] docker-compose.yml が存在する
- [ ] pgvector/pgvector:pg16 イメージを使用している
- [ ] ヘルスチェック設定がある
- [ ] db/init/ に初期化スクリプトがある
- [ ] CREATE EXTENSION vector が初期化に含まれる
- [ ] application.yml にDB接続設定が追加されている
- [ ] spring.datasource 設定がある
- [ ] spring.jpa 設定がある
- [ ] spring.flyway 設定がある
