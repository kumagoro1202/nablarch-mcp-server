package com.tis.nablarch.mcp.codegen;

import java.util.Map;

/**
 * コード生成エンジンインターフェース。
 *
 * <p>テンプレート展開、知識ベース連携、品質チェックを統合し、
 * Nablarch準拠のコードを生成する。</p>
 */
public interface CodeGenerator {

    /**
     * Nablarch準拠のコードを生成する。
     *
     * @param type 生成対象タイプ（action, form, sql, entity, handler, interceptor）
     * @param name クラス/ファイル名
     * @param appType アプリケーションタイプ（web, rest, batch, messaging）
     * @param specifications タイプ固有パラメータ
     * @return 生成結果
     */
    GenerationResult generate(String type, String name, String appType,
                              Map<String, Object> specifications);
}
