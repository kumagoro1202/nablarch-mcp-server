package com.tis.nablarch.mcp.knowledge;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.core.io.DefaultResourceLoader;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * NablarchKnowledgeBase統合テスト。
 * 実際のYAMLファイルをロードし、各カテゴリの検索精度を検証する。
 */
class NablarchKnowledgeBaseIntegrationTest {

    private static NablarchKnowledgeBase kb;

    @BeforeAll
    static void setUp() throws IOException {
        kb = new NablarchKnowledgeBase(
                new DefaultResourceLoader(),
                "classpath:knowledge/");
        kb.initialize();
    }

    // ========== Phase 2: 精度評価 ==========

    @Nested
    class データバインド検索 {
        @Test
        void ObjectMapperFactoryで検索できる() {
            List<String> results = kb.search("ObjectMapperFactory", null);
            assertFalse(results.isEmpty(), "ObjectMapperFactoryの検索結果が空");
            assertTrue(results.stream().anyMatch(r -> r.contains("ObjectMapperFactory")));
        }

        @Test
        void データバインドで検索できる() {
            List<String> results = kb.search("データバインド", null);
            assertFalse(results.isEmpty(), "データバインドの検索結果が空");
        }

        @Test
        void Csvアノテーションで検索できる() {
            List<String> results = kb.search("@Csv", null);
            assertFalse(results.isEmpty(), "@Csvの検索結果が空");
            assertTrue(results.stream().anyMatch(r -> r.contains("Csv")));
        }

        @Test
        void CsvDataBindConfigで検索できる() {
            List<String> results = kb.search("CsvDataBindConfig", null);
            assertFalse(results.isEmpty(), "CsvDataBindConfigの検索結果が空");
            assertTrue(results.stream().anyMatch(r ->
                    r.contains("nablarch.common.databind.csv.CsvDataBindConfig")));
        }

        @Test
        void FixedLengthで検索できる() {
            List<String> results = kb.search("FixedLength", null);
            assertFalse(results.isEmpty(), "FixedLengthの検索結果が空");
        }
    }

    @Nested
    class バリデーション検索 {
        @Test
        void ValidatorUtilで検索できる() {
            List<String> results = kb.search("ValidatorUtil", null);
            assertFalse(results.isEmpty(), "ValidatorUtilの検索結果が空");
            assertTrue(results.stream().anyMatch(r ->
                    r.contains("nablarch.core.validation.ee.ValidatorUtil")));
        }

        @Test
        void Requiredアノテーションで検索できる() {
            List<String> results = kb.search("@Required", null);
            assertFalse(results.isEmpty(), "@Requiredの検索結果が空");
        }

        @Test
        void ドメインバリデーションで検索できる() {
            List<String> results = kb.search("ドメインバリデーション", null);
            assertFalse(results.isEmpty(), "ドメインバリデーションの検索結果が空");
        }

        @Test
        void SystemCharで検索できる() {
            List<String> results = kb.search("SystemChar", null);
            assertFalse(results.isEmpty(), "SystemCharの検索結果が空");
        }

        @Test
        void NumberRangeで検索できる() {
            List<String> results = kb.search("NumberRange", null);
            assertFalse(results.isEmpty(), "NumberRangeの検索結果が空");
            assertTrue(results.stream().anyMatch(r ->
                    r.contains("nablarch.core.validation.ee.NumberRange")));
        }
    }

    @Nested
    class メール検索 {
        @Test
        void MailRequesterで検索できる() {
            List<String> results = kb.search("MailRequester", null);
            assertFalse(results.isEmpty(), "MailRequesterの検索結果が空");
            assertTrue(results.stream().anyMatch(r ->
                    r.contains("nablarch.common.mail.MailRequester")));
        }

        @Test
        void メール送信で検索できる() {
            List<String> results = kb.search("メール送信", null);
            assertFalse(results.isEmpty(), "メール送信の検索結果が空");
        }

        @Test
        void TemplateMailContextで検索できる() {
            List<String> results = kb.search("TemplateMailContext", null);
            assertFalse(results.isEmpty(), "TemplateMailContextの検索結果が空");
        }
    }

    @Nested
    class メッセージ管理検索 {
        @Test
        void MessageUtilで検索できる() {
            List<String> results = kb.search("MessageUtil", null);
            assertFalse(results.isEmpty(), "MessageUtilの検索結果が空");
            assertTrue(results.stream().anyMatch(r ->
                    r.contains("nablarch.core.message.MessageUtil")));
        }

        @Test
        void ApplicationExceptionで検索できる() {
            List<String> results = kb.search("ApplicationException", null);
            assertFalse(results.isEmpty(), "ApplicationExceptionの検索結果が空");
        }

        @Test
        void CodeUtilで検索できる() {
            List<String> results = kb.search("CodeUtil", null);
            assertFalse(results.isEmpty(), "CodeUtilの検索結果が空");
            assertTrue(results.stream().anyMatch(r ->
                    r.contains("nablarch.common.code.CodeUtil")));
        }

        @Test
        void CodeValueアノテーションで検索できる() {
            List<String> results = kb.search("CodeValue", null);
            assertFalse(results.isEmpty(), "CodeValueの検索結果が空");
        }
    }

    @Nested
    class ユーティリティ検索 {
        @Test
        void BusinessDateUtilで検索できる() {
            List<String> results = kb.search("BusinessDateUtil", null);
            assertFalse(results.isEmpty(), "BusinessDateUtilの検索結果が空");
            assertTrue(results.stream().anyMatch(r ->
                    r.contains("nablarch.core.date.BusinessDateUtil")));
        }

        @Test
        void BeanUtilで検索できる() {
            List<String> results = kb.search("BeanUtil", null);
            assertFalse(results.isEmpty(), "BeanUtilの検索結果が空");
            assertTrue(results.stream().anyMatch(r ->
                    r.contains("nablarch.core.beans.BeanUtil")));
        }

        @Test
        void StringUtilで検索できる() {
            List<String> results = kb.search("StringUtil", null);
            assertFalse(results.isEmpty(), "StringUtilの検索結果が空");
        }

        @Test
        void FilePathSettingで検索できる() {
            List<String> results = kb.search("FilePathSetting", null);
            assertFalse(results.isEmpty(), "FilePathSettingの検索結果が空");
        }
    }

    @Nested
    class ログ検索 {
        @Test
        void LoggerManagerで検索できる() {
            List<String> results = kb.search("LoggerManager", null);
            assertFalse(results.isEmpty(), "LoggerManagerの検索結果が空");
            assertTrue(results.stream().anyMatch(r ->
                    r.contains("nablarch.core.log.LoggerManager")));
        }

        @Test
        void ログ出力で検索できる() {
            List<String> results = kb.search("ログ出力", null);
            assertFalse(results.isEmpty(), "ログ出力の検索結果が空");
        }

        @Test
        void SQLログで検索できる() {
            List<String> results = kb.search("SQLログ", null);
            assertFalse(results.isEmpty(), "SQLログの検索結果が空");
        }

        @Test
        void JsonLogFormatterで検索できる() {
            List<String> results = kb.search("JsonLogFormatter", null);
            assertFalse(results.isEmpty(), "JsonLogFormatterの検索結果が空");
        }
    }

    @Nested
    class セキュリティ検索 {
        @Test
        void SessionUtilで検索できる() {
            List<String> results = kb.search("SessionUtil", null);
            assertFalse(results.isEmpty(), "SessionUtilの検索結果が空");
            assertTrue(results.stream().anyMatch(r ->
                    r.contains("nablarch.common.web.session.SessionUtil")));
        }

        @Test
        void CSRF防止で検索できる() {
            List<String> results = kb.search("CSRF", null);
            assertFalse(results.isEmpty(), "CSRFの検索結果が空");
        }

        @Test
        void 排他制御で検索できる() {
            List<String> results = kb.search("排他制御", null);
            assertFalse(results.isEmpty(), "排他制御の検索結果が空");
        }

        @Test
        void PermissionUtilで検索できる() {
            List<String> results = kb.search("PermissionUtil", null);
            assertFalse(results.isEmpty(), "PermissionUtilの検索結果が空");
        }

        @Test
        void 二重サブミットで検索できる() {
            List<String> results = kb.search("二重サブミット", null);
            assertFalse(results.isEmpty(), "二重サブミットの検索結果が空");
        }
    }

    @Nested
    class 既存知識の検索が壊れていないこと {
        @Test
        void UniversalDaoで検索できる() {
            List<String> results = kb.search("UniversalDao", null);
            assertFalse(results.isEmpty(), "UniversalDaoの検索結果が空");
        }

        @Test
        void ハンドラで検索できる() {
            List<String> results = kb.search("GlobalErrorHandler", null);
            assertFalse(results.isEmpty(), "GlobalErrorHandlerの検索結果が空");
        }

        @Test
        void web用ハンドラカタログが取得できる() {
            var catalog = kb.getHandlerCatalog("web");
            assertNotNull(catalog, "webハンドラカタログがnull");
        }

        @Test
        void 設計パターンが取得できる() {
            var patterns = kb.getAllDesignPatterns();
            assertFalse(patterns.isEmpty(), "設計パターンが空");
        }
    }

    @Nested
    class FQCN精度検証 {
        @ParameterizedTest
        @CsvSource({
                "ObjectMapperFactory, nablarch.common.databind.ObjectMapperFactory",
                "ValidatorUtil, nablarch.core.validation.ee.ValidatorUtil",
                "MailRequester, nablarch.common.mail.MailRequester",
                "MessageUtil, nablarch.core.message.MessageUtil",
                "CodeUtil, nablarch.common.code.CodeUtil",
                "BusinessDateUtil, nablarch.core.date.BusinessDateUtil",
                "BeanUtil, nablarch.core.beans.BeanUtil",
                "LoggerManager, nablarch.core.log.LoggerManager",
                "SessionUtil, nablarch.common.web.session.SessionUtil",
                "PermissionUtil, nablarch.common.permission.PermissionUtil"
        })
        void 主要クラスのFQCNが正しく検索される(String keyword, String expectedFqcn) {
            List<String> results = kb.search(keyword, null);
            assertFalse(results.isEmpty(), keyword + "の検索結果が空");
            assertTrue(results.stream().anyMatch(r -> r.contains(expectedFqcn)),
                    keyword + "の検索結果に" + expectedFqcn + "が含まれない。結果: " + results);
        }
    }
}
