# WBS 2.2.16 メタデータフィルタリング 完了基準チェックリスト

## 実装

- [x] ExtendedSearchFilters record が存在する
- [x] ExtendedSearchFilters に baseFilters, version, fqcnPrefix, dateFrom, dateTo フィールドがある
- [x] ExtendedSearchFilters に NONE 定数がある
- [x] ExtendedSearchFilters に hasExtendedFilters(), hasAnyFilter() メソッドがある
- [x] MetadataFilteringService @Service が存在する
- [x] filter() メソッド: baseFilters の完全一致フィルタ（appType, module, source, sourceType, language）
- [x] filter() メソッド: version 前方一致（"5" → "5u23"にマッチ）
- [x] filter() メソッド: fqcnPrefix 前方一致
- [x] filter() メソッド: dateRange（dateFrom以降 + dateTo以前）
- [x] filter() メソッド: null条件は無視（条件なし扱い）
- [x] computeFacets() メソッド: source, source_type, app_type, module, language の集計
- [x] computeFacets() メソッド: 空リスト/null入力で空マップ返却
- [x] Javadocが日本語で記述されている

## テスト

### ExtendedSearchFiltersTest

- [x] 全フィールドが正しく保持される
- [x] NONE定数は全フィールドがnull
- [x] hasExtendedFilters: version指定時にtrue
- [x] hasExtendedFilters: fqcnPrefix指定時にtrue
- [x] hasExtendedFilters: 日付範囲指定時にtrue
- [x] hasExtendedFilters: 拡張条件なしでfalse
- [x] hasAnyFilter: baseFiltersのみでもtrue
- [x] hasAnyFilter: 拡張条件のみでもtrue
- [x] hasAnyFilter: 全条件なしでfalse
- [x] hasAnyFilter: baseFiltersがNONEの場合false

### MetadataFilteringServiceTest

- [x] ポストフィルタ: appTypeフィルタ
- [x] ポストフィルタ: sourceフィルタ
- [x] ポストフィルタ: languageフィルタ
- [x] ポストフィルタ: 複合条件（source + sourceType）
- [x] ポストフィルタ: バージョン前方一致 "6" → "6u3"
- [x] ポストフィルタ: バージョン前方一致 "5" → "5u23"
- [x] ポストフィルタ: FQCN前方一致 "nablarch.fw.web"
- [x] ポストフィルタ: FQCN前方一致 "nablarch.fw"（広範囲）
- [x] ポストフィルタ: 日付範囲（from + to）
- [x] ポストフィルタ: dateFromのみ指定
- [x] ポストフィルタ: dateToのみ指定
- [x] null条件は無視される
- [x] NONE条件は全件返却
- [x] null入力は空リスト
- [x] 空リスト入力は空リスト
- [x] metadataがnullの結果はフィルタされる
- [x] ファセット集計: 正常ケース（全5ファセットキー）
- [x] ファセット集計: source_typeファセット
- [x] ファセット集計: moduleファセット
- [x] ファセット集計: 空リスト → 空マップ
- [x] ファセット集計: null → 空マップ
- [x] ファセット集計: metadataがnullの結果はスキップ
