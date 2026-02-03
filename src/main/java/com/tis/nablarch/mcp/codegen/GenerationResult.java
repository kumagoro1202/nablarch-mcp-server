package com.tis.nablarch.mcp.codegen;

import java.util.List;

/**
 * コード生成結果。
 *
 * @param type 生成対象タイプ
 * @param name クラス/ファイル名
 * @param appType アプリケーションタイプ
 * @param files 生成されたファイル一覧
 * @param conventionsApplied 適用されたNablarch規約
 * @param dependencies 必要な依存モジュール
 * @param warnings 警告メッセージ
 */
public record GenerationResult(
        String type,
        String name,
        String appType,
        List<GeneratedFile> files,
        List<String> conventionsApplied,
        List<String> dependencies,
        List<String> warnings) {

    /**
     * 生成されたファイル。
     *
     * @param path 推奨ファイルパス
     * @param fileName ファイル名
     * @param content 生成されたコード内容
     * @param language 言語（java, xml, sql）
     */
    public record GeneratedFile(
            String path,
            String fileName,
            String content,
            String language) {
    }
}
