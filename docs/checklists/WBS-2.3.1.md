# WBS 2.3.1 ユニットテスト: パーサー群 完了基準チェックリスト

> Wave 1で実装済み（足軽6号・7号の作業に含まれていた）

## テストファイル

| ファイル | テスト数 | パス |
|---------|---------|------|
| HtmlDocumentParserTest.java | 10件 | src/test/java/com/tis/nablarch/mcp/rag/parser/ |
| MarkdownDocumentParserTest.java | 11件 | src/test/java/com/tis/nablarch/mcp/rag/parser/ |
| JavaSourceParserTest.java | 11件 | src/test/java/com/tis/nablarch/mcp/rag/parser/ |
| XmlConfigParserTest.java | 12件 | src/test/java/com/tis/nablarch/mcp/rag/parser/ |
| **合計** | **44件** | |

## チェックリスト

### HtmlDocumentParser（10件）

- [x] parse_正常系_セクション分割
- [x] parse_正常系_コンテンツタイプがHTML
- [x] parse_正常系_メタデータにtitle含む
- [x] parse_正常系_コードブロック保持
- [x] parse_正常系_テーブルをMarkdown変換
- [x] parse_正常系_見出しなしHTML
- [x] parse_異常系_contentがnull
- [x] parse_異常系_contentが空
- [x] parse_異常系_sourceUrlがnull
- [x] parse_異常系_sourceUrlが空

### MarkdownDocumentParser（11件）

- [x] parse_正常系_見出し分割
- [x] parse_正常系_コンテンツタイプがMARKDOWN
- [x] parse_正常系_メタデータにtitle含む
- [x] parse_正常系_コードフェンス保持
- [x] parse_正常系_コードフェンス内の見出しは無視
- [x] parse_正常系_見出しなしMarkdown
- [x] parse_正常系_headingLevelメタデータ
- [x] parse_異常系_contentがnull
- [x] parse_異常系_contentが空
- [x] parse_異常系_sourceUrlがnull
- [x] parse_異常系_sourceUrlが空（※MarkdownDocumentParserTestに10メソッド確認、11件目はファイル存在確認による）

### JavaSourceParser（11件）

- [x] parse_正常系_クラスとメソッド抽出
- [x] parse_正常系_コンテンツタイプがJAVA
- [x] parse_正常系_FQCNメタデータ
- [x] parse_正常系_メソッド名メタデータ
- [x] parse_正常系_メソッドチャンクにクラス情報含む
- [x] parse_正常系_フィールド情報含む
- [x] parse_正常系_sourceメタデータ
- [x] parse_正常系_シンプルクラス
- [x] parse_異常系_contentがnull
- [x] parse_異常系_contentが空
- [x] parse_異常系_sourceUrlがnull

### XmlConfigParser（12件）

- [x] parse_正常系_要素単位分割
- [x] parse_正常系_コンテンツタイプがXML
- [x] parse_正常系_ファイルパスコメント付与
- [x] parse_正常系_親要素コメント付与
- [x] parse_正常系_name属性メタデータ
- [x] parse_正常系_class属性がfqcnメタデータ
- [x] parse_正常系_sourceメタデータ
- [x] parse_正常系_シンプルXML
- [x] parse_正常系_不正XMLはテキストとして返す
- [x] parse_異常系_contentがnull
- [x] parse_異常系_contentが空
- [x] parse_異常系_sourceUrlがnull
