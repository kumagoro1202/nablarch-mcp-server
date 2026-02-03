package com.tis.nablarch.mcp.resources;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link VersionResourceProvider} のユニットテスト。
 *
 * <p>version リソースプロバイダのテスト。
 * フレームワークバージョン情報の取得を検証する。</p>
 */
class VersionResourceProviderTest {

    private VersionResourceProvider provider;

    /**
     * テスト前にプロバイダを初期化する。
     *
     * @throws IOException YAMLファイルの読み込みに失敗した場合
     */
    @BeforeEach
    void setUp() throws IOException {
        provider = new VersionResourceProvider();
        provider.init();
    }

    // ==================== getVersionInfo テスト ====================

    /**
     * バージョン情報が正常にJSON形式で返却されることを検証する。
     */
    @Test
    void getVersionInfo_returnsValidJson() {
        String result = provider.getVersionInfo();
        assertNotNull(result);
        assertTrue(result.contains("\"type\" : \"version_info\""));
    }

    /**
     * バージョン情報にフレームワーク名が含まれることを検証する。
     */
    @Test
    void getVersionInfo_containsFrameworkName() {
        String result = provider.getVersionInfo();
        assertTrue(result.contains("\"framework_name\" : \"Nablarch\""));
    }

    /**
     * バージョン情報に最新バージョンが含まれることを検証する。
     */
    @Test
    void getVersionInfo_containsLatestVersion() {
        String result = provider.getVersionInfo();
        assertTrue(result.contains("\"latest_version\" : \"6u3\""));
    }

    /**
     * バージョン情報にリリース日が含まれることを検証する。
     */
    @Test
    void getVersionInfo_containsReleaseDate() {
        String result = provider.getVersionInfo();
        assertTrue(result.contains("\"release_date\""));
    }

    /**
     * バージョン情報にサポートバージョン一覧が含まれることを検証する。
     */
    @Test
    void getVersionInfo_containsSupportedVersions() {
        String result = provider.getVersionInfo();
        assertTrue(result.contains("\"supported_versions\""));
        assertTrue(result.contains("6u3") || result.contains("5u24"));
    }

    /**
     * バージョン情報にプラットフォーム情報が含まれることを検証する。
     */
    @Test
    void getVersionInfo_containsPlatforms() {
        String result = provider.getVersionInfo();
        assertTrue(result.contains("\"platforms\""));
    }

    /**
     * プラットフォーム情報にアプリケーションサーバが含まれることを検証する。
     */
    @Test
    void getVersionInfo_containsApplicationServers() {
        String result = provider.getVersionInfo();
        assertTrue(result.contains("application_server") ||
                   result.contains("Tomcat") ||
                   result.contains("WildFly"));
    }

    /**
     * プラットフォーム情報にデータベースが含まれることを検証する。
     */
    @Test
    void getVersionInfo_containsDatabases() {
        String result = provider.getVersionInfo();
        assertTrue(result.contains("database") ||
                   result.contains("Oracle") ||
                   result.contains("PostgreSQL"));
    }

    /**
     * バージョン情報にBOM情報が含まれることを検証する。
     */
    @Test
    void getVersionInfo_containsBom() {
        String result = provider.getVersionInfo();
        assertTrue(result.contains("\"bom\""));
        assertTrue(result.contains("nablarch-bom"));
    }

    /**
     * バージョン情報にモジュール一覧が含まれることを検証する。
     */
    @Test
    void getVersionInfo_containsModules() {
        String result = provider.getVersionInfo();
        assertTrue(result.contains("\"modules\""));
        assertTrue(result.contains("\"total_modules\""));
    }

    /**
     * モジュール一覧にartifact_id情報が含まれることを検証する。
     */
    @Test
    void getVersionInfo_modulesContainArtifactId() {
        String result = provider.getVersionInfo();
        assertTrue(result.contains("\"artifact_id\""));
        assertTrue(result.contains("nablarch-"));
    }

    /**
     * バージョン情報にリンク情報が含まれることを検証する。
     */
    @Test
    void getVersionInfo_containsLinks() {
        String result = provider.getVersionInfo();
        assertTrue(result.contains("\"links\""));
    }

    /**
     * リンク情報に公式ドキュメントURLが含まれることを検証する。
     */
    @Test
    void getVersionInfo_containsOfficialDocsLink() {
        String result = provider.getVersionInfo();
        assertTrue(result.contains("official_docs") ||
                   result.contains("nablarch.github.io"));
    }

    /**
     * リンク情報にGitHub組織URLが含まれることを検証する。
     */
    @Test
    void getVersionInfo_containsGithubLink() {
        String result = provider.getVersionInfo();
        assertTrue(result.contains("github_org") ||
                   result.contains("github.com/nablarch"));
    }
}
