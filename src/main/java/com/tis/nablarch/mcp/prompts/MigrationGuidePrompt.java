package com.tis.nablarch.mcp.prompts;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * migration-guide Promptの実装。
 *
 * <p>Nablarchバージョン間の移行ガイドを生成する。
 * module-catalog.yaml から全モジュール情報と依存関係を読み込み、
 * 移行時の確認事項を体系的に出力する。</p>
 */
@Component
public class MigrationGuidePrompt {

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    private Map<String, Object> moduleCatalog;

    /**
     * 知識YAMLファイルを読み込んで初期化する。
     *
     * @throws IOException YAMLファイルの読み込みに失敗した場合
     */
    @PostConstruct
    @SuppressWarnings("unchecked")
    void init() throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/knowledge/module-catalog.yaml")) {
            moduleCatalog = YAML.readValue(is, Map.class);
        }
    }

    /**
     * Promptを実行してバージョン移行ガイドを生成する。
     *
     * @param arguments MCP Promptの引数マップ（from_version, to_version 必須）
     * @return バージョン移行ガイドを含むPrompt結果
     * @throws IllegalArgumentException 必須引数が未指定の場合
     */
    @SuppressWarnings("unchecked")
    public McpSchema.GetPromptResult execute(Map<String, String> arguments) {
        String fromVersion = arguments != null ? arguments.get("from_version") : null;
        String toVersion = arguments != null ? arguments.get("to_version") : null;

        if (fromVersion == null || fromVersion.isBlank()) {
            throw new IllegalArgumentException("from_version は必須です。移行元のNablarchバージョンを指定してください。");
        }
        if (toVersion == null || toVersion.isBlank()) {
            throw new IllegalArgumentException("to_version は必須です。移行先のNablarchバージョンを指定してください。");
        }

        var sb = new StringBuilder();
        sb.append("# Nablarch ").append(fromVersion).append(" → ").append(toVersion).append(" 移行ガイド\n\n");

        sb.append("## 概要\n\n");
        sb.append("Nablarch ").append(fromVersion).append(" から ").append(toVersion)
          .append(" への移行に必要な確認事項と手順を示します。\n\n");

        // モジュール一覧
        List<Map<String, Object>> modules = (List<Map<String, Object>>) moduleCatalog.get("modules");
        if (modules != null) {
            sb.append("## モジュール一覧\n\n");
            sb.append("移行対象のNablarchモジュール（全").append(modules.size()).append("モジュール）:\n\n");
            sb.append("| モジュール | artifactId | カテゴリ | 説明 |\n");
            sb.append("|----------|-----------|---------|------|\n");
            for (var m : modules) {
                sb.append("| ").append(m.get("name"))
                  .append(" | `").append(m.get("artifact_id")).append("`")
                  .append(" | ").append(m.get("category"))
                  .append(" | ").append(m.get("description"))
                  .append(" |\n");
            }
            sb.append("\n");

            // 主要クラスの確認事項
            sb.append("## 主要クラスの確認事項\n\n");
            sb.append("以下の主要クラスについて、バージョン間でのAPI変更を確認してください。\n\n");
            for (var m : modules) {
                List<Map<String, Object>> keyClasses = (List<Map<String, Object>>) m.get("key_classes");
                if (keyClasses != null && !keyClasses.isEmpty()) {
                    sb.append("### ").append(m.get("name")).append("\n\n");
                    for (var kc : keyClasses) {
                        sb.append("- `").append(kc.get("fqcn")).append("` — ").append(kc.get("description")).append("\n");
                    }
                    sb.append("\n");
                }
            }

            // 依存関係の確認
            sb.append("## 依存関係の確認\n\n");
            sb.append("移行時にpom.xmlのバージョンを更新する必要があるモジュール:\n\n");
            for (var m : modules) {
                List<String> deps = (List<String>) m.get("dependencies");
                if (deps != null && !deps.isEmpty()) {
                    sb.append("- **").append(m.get("name")).append("** → 依存: ").append(String.join(", ", deps)).append("\n");
                }
            }
            sb.append("\n");
        }

        // 一般的な移行手順
        sb.append("## 一般的な移行手順\n\n");
        sb.append("1. **BOMバージョンの更新**: `nablarch-bom` の parent バージョンを `").append(toVersion).append("` に変更\n");
        sb.append("2. **非互換変更の確認**: リリースノートで破壊的変更を確認\n");
        sb.append("3. **コンパイル確認**: `mvn compile` でコンパイルエラーを検出\n");
        sb.append("4. **テスト実行**: `mvn test` で全テストが通ることを確認\n");
        sb.append("5. **handler-catalog.yaml の確認**: ハンドラキュー構成が新バージョンの推奨と一致しているか確認\n");
        sb.append("6. **XML設定の確認**: コンポーネント定義のクラス名やプロパティ名が変更されていないか確認\n");
        sb.append("7. **動作確認**: 主要機能の結合テストを実施\n\n");

        return new McpSchema.GetPromptResult(
            "Nablarch " + fromVersion + " → " + toVersion + " 移行ガイド",
            List.of(new McpSchema.PromptMessage(
                McpSchema.Role.USER,
                new McpSchema.TextContent(sb.toString())
            ))
        );
    }
}
