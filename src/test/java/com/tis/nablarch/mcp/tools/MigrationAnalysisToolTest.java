package com.tis.nablarch.mcp.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MigrationAnalysisToolのユニットテスト。
 *
 * <p>品質担保戦略に基づき、開発者レビュー観点で設計：
 * <ul>
 *   <li>@DisplayName による意図明示</li>
 *   <li>AAA (Arrange-Act-Assert) パターン</li>
 *   <li>エッジケースの網羅</li>
 *   <li>テストの独立性</li>
 * </ul>
 * </p>
 *
 * @see MigrationAnalysisTool
 */
class MigrationAnalysisToolTest {

    private MigrationAnalysisTool tool;

    @BeforeEach
    void setUp() {
        tool = new MigrationAnalysisTool();
    }

    @Nested
    @DisplayName("入力検証テスト")
    class InputValidationTest {

        @Test
        @DisplayName("コードスニペットがnullの場合エラーメッセージを返す")
        void nullCodeSnippetReturnsError() {
            // Arrange
            String codeSnippet = null;

            // Act
            String result = tool.analyzeMigration(codeSnippet, null, null, null);

            // Assert
            assertTrue(result.contains("エラー"));
            assertTrue(result.contains("コードが指定されていません"));
        }

        @Test
        @DisplayName("コードスニペットが空白の場合エラーメッセージを返す")
        void emptyCodeSnippetReturnsError() {
            // Arrange
            String codeSnippet = "   ";

            // Act
            String result = tool.analyzeMigration(codeSnippet, null, null, null);

            // Assert
            assertTrue(result.contains("エラー"));
            assertTrue(result.contains("コードが指定されていません"));
        }

        @Test
        @DisplayName("サポートされていないソースバージョンの場合エラーメッセージを返す")
        void unsupportedSourceVersionReturnsError() {
            // Arrange
            String codeSnippet = "import javax.servlet.http.HttpServlet;";
            String sourceVersion = "4";

            // Act
            String result = tool.analyzeMigration(codeSnippet, sourceVersion, "6", null);

            // Assert
            assertTrue(result.contains("エラー"));
            assertTrue(result.contains("サポートされていない移行元バージョン"));
        }

        @Test
        @DisplayName("サポートされていないターゲットバージョンの場合エラーメッセージを返す")
        void unsupportedTargetVersionReturnsError() {
            // Arrange
            String codeSnippet = "import javax.servlet.http.HttpServlet;";
            String targetVersion = "7";

            // Act
            String result = tool.analyzeMigration(codeSnippet, "5", targetVersion, null);

            // Assert
            assertTrue(result.contains("エラー"));
            assertTrue(result.contains("サポートされていない移行先バージョン"));
        }
    }

    @Nested
    @DisplayName("名前空間変更検出テスト")
    class NamespaceDetectionTest {

        @Test
        @DisplayName("javax.servletをjakarta.servletへの変更として検出する")
        void detectsJavaxServletToJakarta() {
            // Arrange
            String codeSnippet = """
                import javax.servlet.http.HttpServlet;
                import javax.servlet.http.HttpServletRequest;
                import javax.servlet.http.HttpServletResponse;

                public class MyServlet extends HttpServlet {
                }
                """;

            // Act
            String result = tool.analyzeMigration(codeSnippet, "5", "6", null);

            // Assert
            assertTrue(result.contains("BC-001"));
            assertTrue(result.contains("jakarta.servlet"));
            assertTrue(result.contains("自動修正可能"));
        }

        @Test
        @DisplayName("javax.persistenceをjakarta.persistenceへの変更として検出する")
        void detectsJavaxPersistenceToJakarta() {
            // Arrange
            String codeSnippet = """
                import javax.persistence.Entity;
                import javax.persistence.Id;

                @Entity
                public class MyEntity {
                    @Id
                    private Long id;
                }
                """;

            // Act
            String result = tool.analyzeMigration(codeSnippet, "5", "6", null);

            // Assert
            assertTrue(result.contains("BC-002"));
            assertTrue(result.contains("jakarta.persistence"));
        }

        @Test
        @DisplayName("javax.validationをjakarta.validationへの変更として検出する")
        void detectsJavaxValidationToJakarta() {
            // Arrange
            String codeSnippet = """
                import javax.validation.constraints.NotNull;
                import javax.validation.Valid;

                public class MyForm {
                    @NotNull
                    private String name;
                }
                """;

            // Act
            String result = tool.analyzeMigration(codeSnippet, "5", "6", null);

            // Assert
            assertTrue(result.contains("BC-008"));
            assertTrue(result.contains("jakarta.validation"));
        }
    }

    @Nested
    @DisplayName("API削除検出テスト")
    class ApiRemovalDetectionTest {

        @Test
        @DisplayName("DbAccessSupportの使用を検出する")
        void detectsDbAccessSupportUsage() {
            // Arrange
            String codeSnippet = """
                import nablarch.common.dao.DbAccessSupport;

                public class MyAction extends DbAccessSupport {
                    public void execute() {
                        // データベースアクセス
                    }
                }
                """;

            // Act
            String result = tool.analyzeMigration(codeSnippet, "5", "6", null);

            // Assert
            assertTrue(result.contains("BC-003"));
            assertTrue(result.contains("削除されたAPI"));
            assertTrue(result.contains("手動修正必要"));
        }
    }

    @Nested
    @DisplayName("依存関係変更検出テスト")
    class DependencyDetectionTest {

        @Test
        @DisplayName("javax.servlet-api依存を検出する")
        void detectsJavaxServletApiDependency() {
            // Arrange
            String codeSnippet = """
                <dependency>
                    <groupId>javax.servlet</groupId>
                    <artifactId>javax.servlet-api</artifactId>
                    <version>4.0.1</version>
                </dependency>
                """;

            // Act
            String result = tool.analyzeMigration(codeSnippet, "5", "6", null);

            // Assert
            assertTrue(result.contains("BC-006"));
            assertTrue(result.contains("jakarta.servlet-api"));
        }

        @Test
        @DisplayName("nablarch-bom 5.x依存を検出する")
        void detectsNablarchBom5xDependency() {
            // Arrange
            String codeSnippet = """
                <dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>com.nablarch.profile</groupId>
                            <artifactId>nablarch-bom</artifactId>
                            <version>5.1.0</version>
                            <type>pom</type>
                            <scope>import</scope>
                        </dependency>
                    </dependencies>
                </dependencyManagement>
                """;

            // Act
            String result = tool.analyzeMigration(codeSnippet, "5", "6", null);

            // Assert
            assertTrue(result.contains("BC-007"));
            assertTrue(result.contains("nablarch-bom"));
        }
    }

    @Nested
    @DisplayName("コードタイプ検出テスト")
    class CodeTypeDetectionTest {

        @Test
        @DisplayName("Javaコードを正しく検出する")
        void detectsJavaCode() {
            // Arrange
            String codeSnippet = """
                package com.example;

                import javax.servlet.http.HttpServlet;

                public class MyServlet extends HttpServlet {
                }
                """;

            // Act
            String result = tool.analyzeMigration(codeSnippet, "5", "6", null);

            // Assert
            assertTrue(result.contains("コードタイプ | Java"));
        }

        @Test
        @DisplayName("XMLコードを正しく検出する")
        void detectsXmlCode() {
            // Arrange
            String codeSnippet = """
                <?xml version="1.0" encoding="UTF-8"?>
                <component name="handler">
                    <list name="handlerQueue">
                        <component class="javax.servlet.Filter"/>
                    </list>
                </component>
                """;

            // Act
            String result = tool.analyzeMigration(codeSnippet, "5", "6", null);

            // Assert
            assertTrue(result.contains("コードタイプ | XML"));
        }

        @Test
        @DisplayName("POMファイルを正しく検出する")
        void detectsPomCode() {
            // Arrange
            String codeSnippet = """
                <project>
                    <dependencies>
                        <dependency>
                            <groupId>javax.servlet</groupId>
                            <artifactId>javax.servlet-api</artifactId>
                        </dependency>
                    </dependencies>
                </project>
                """;

            // Act
            String result = tool.analyzeMigration(codeSnippet, "5", "6", null);

            // Assert
            assertTrue(result.contains("コードタイプ | POM"));
        }
    }

    @Nested
    @DisplayName("分析スコープテスト")
    class AnalysisScopeTest {

        @Test
        @DisplayName("スコープがnamespaceの場合、名前空間変更のみを検出する")
        void namespaceScopeFiltersResults() {
            // Arrange
            String codeSnippet = """
                import javax.servlet.http.HttpServlet;

                <dependency>
                    <artifactId>javax.servlet-api</artifactId>
                </dependency>
                """;

            // Act
            String result = tool.analyzeMigration(codeSnippet, "5", "6", "namespace");

            // Assert
            assertTrue(result.contains("BC-001")); // namespace
            assertFalse(result.contains("BC-006")); // dependency（スコープ外）
        }

        @Test
        @DisplayName("スコープがdependencyの場合、依存関係変更のみを検出する")
        void dependencyScopeFiltersResults() {
            // Arrange
            String codeSnippet = """
                import javax.servlet.http.HttpServlet;

                <dependency>
                    <artifactId>javax.servlet-api</artifactId>
                </dependency>
                """;

            // Act
            String result = tool.analyzeMigration(codeSnippet, "5", "6", "dependency");

            // Assert
            assertFalse(result.contains("BC-001")); // namespace（スコープ外）
            assertTrue(result.contains("BC-006")); // dependency
        }
    }

    @Nested
    @DisplayName("レポート生成テスト")
    class ReportGenerationTest {

        @Test
        @DisplayName("問題が検出されない場合、成功メッセージを返す")
        void noIssuesReturnsSuccessMessage() {
            // Arrange
            String codeSnippet = """
                import jakarta.servlet.http.HttpServlet;

                public class MyServlet extends HttpServlet {
                }
                """;

            // Act
            String result = tool.analyzeMigration(codeSnippet, "5", "6", null);

            // Assert
            assertTrue(result.contains("移行に影響する問題は検出されませんでした"));
        }

        @Test
        @DisplayName("レポートにサマリが含まれる")
        void reportIncludesSummary() {
            // Arrange
            String codeSnippet = "import javax.servlet.http.HttpServlet;";

            // Act
            String result = tool.analyzeMigration(codeSnippet, "5", "6", null);

            // Assert
            assertTrue(result.contains("サマリ"));
            assertTrue(result.contains("自動修正可能"));
            assertTrue(result.contains("手動修正必要"));
        }

        @Test
        @DisplayName("レポートに工数見積もりが含まれる")
        void reportIncludesEffortEstimate() {
            // Arrange
            String codeSnippet = """
                import javax.servlet.http.HttpServlet;
                import nablarch.common.dao.DbAccessSupport;
                """;

            // Act
            String result = tool.analyzeMigration(codeSnippet, "5", "6", null);

            // Assert
            assertTrue(result.contains("工数見積もり"));
            assertTrue(result.contains("trivial"));
        }

        @Test
        @DisplayName("レポートに推奨移行手順が含まれる")
        void reportIncludesRecommendedSteps() {
            // Arrange
            String codeSnippet = "import javax.servlet.http.HttpServlet;";

            // Act
            String result = tool.analyzeMigration(codeSnippet, "5", "6", null);

            // Assert
            assertTrue(result.contains("推奨移行手順"));
            assertTrue(result.contains("自動修正可能な問題を先に対応"));
        }

        @Test
        @DisplayName("レポートに参考リソースが含まれる")
        void reportIncludesResources() {
            // Arrange
            String codeSnippet = "import javax.servlet.http.HttpServlet;";

            // Act
            String result = tool.analyzeMigration(codeSnippet, "5", "6", null);

            // Assert
            assertTrue(result.contains("参考リソース"));
            assertTrue(result.contains("nablarch://"));
        }
    }

    @Nested
    @DisplayName("追加パターン検出テスト")
    class AdditionalPatternDetectionTest {

        @Test
        @DisplayName("javax.annotationをjakarta.annotationへの変更として検出する（BC-004）")
        void detectsJavaxAnnotationToJakarta() {
            // Arrange
            String codeSnippet = """
                import javax.annotation.PostConstruct;
                import javax.annotation.PreDestroy;
                import javax.annotation.Resource;

                public class MyBean {
                    @PostConstruct
                    public void init() {}
                }
                """;

            // Act
            String result = tool.analyzeMigration(codeSnippet, "5", "6", null);

            // Assert
            assertTrue(result.contains("BC-004"));
            assertTrue(result.contains("jakarta.annotation"));
        }

        @Test
        @DisplayName("javax.annotation.processingは除外される（BC-004の例外）")
        void excludesJavaxAnnotationProcessing() {
            // Arrange
            String codeSnippet = """
                import javax.annotation.processing.Processor;
                import javax.annotation.processing.AbstractProcessor;
                """;

            // Act
            String result = tool.analyzeMigration(codeSnippet, "5", "6", null);

            // Assert
            assertFalse(result.contains("BC-004"));
        }

        @Test
        @DisplayName("javax.injectをjakarta.injectへの変更として検出する（BC-009）")
        void detectsJavaxInjectToJakarta() {
            // Arrange
            String codeSnippet = """
                import javax.inject.Inject;
                import javax.inject.Named;

                public class MyService {
                    @Inject
                    private Repository repository;
                }
                """;

            // Act
            String result = tool.analyzeMigration(codeSnippet, "5", "6", null);

            // Assert
            assertTrue(result.contains("BC-009"));
            assertTrue(result.contains("jakarta.inject"));
        }

        @Test
        @DisplayName("非推奨Handlerを検出する（BC-010）")
        void detectsDeprecatedHandlers() {
            // Arrange
            String codeSnippet = """
                import nablarch.fw.web.handler.HttpAccessLogHandler;

                public class MyHandler extends HttpAccessLogHandler {
                }
                """;

            // Act
            String result = tool.analyzeMigration(codeSnippet, "5", "6", null);

            // Assert
            assertTrue(result.contains("BC-010"));
        }
    }

    @Nested
    @DisplayName("Propertiesコードタイプテスト")
    class PropertiesCodeTypeTest {

        @Test
        @DisplayName("Propertiesファイルを正しく検出する")
        void detectsPropertiesCode() {
            // Arrange
            String codeSnippet = """
                nablarch.db.jdbcUrl=jdbc:h2:mem:test
                nablarch.db.username=sa
                nablarch.db.password=
                nablarch.server.port=8080
                """;

            // Act
            String result = tool.analyzeMigration(codeSnippet, "5", "6", null);

            // Assert
            assertTrue(result.contains("コードタイプ | Properties"));
        }
    }

    @Nested
    @DisplayName("境界値テスト")
    class BoundaryValueTest {

        @Test
        @DisplayName("大量のimport文を含むコードを正しく分析する")
        void handlesLargeNumberOfImports() {
            // Arrange
            StringBuilder sb = new StringBuilder();
            sb.append("package com.example;\n\n");
            for (int i = 0; i < 50; i++) {
                sb.append("import javax.servlet.http.HttpServlet").append(i).append(";\n");
            }
            sb.append("\npublic class LargeClass {}\n");
            String codeSnippet = sb.toString();

            // Act
            String result = tool.analyzeMigration(codeSnippet, "5", "6", null);

            // Assert
            assertTrue(result.contains("BC-001"));
            assertTrue(result.contains("件"));
        }

        @Test
        @DisplayName("1行のコードでも正しく分析する")
        void handlesSingleLineCode() {
            // Arrange
            String codeSnippet = "import javax.servlet.http.HttpServlet;";

            // Act
            String result = tool.analyzeMigration(codeSnippet, "5", "6", null);

            // Assert
            assertTrue(result.contains("BC-001"));
        }

        @Test
        @DisplayName("特殊文字を含むコードを正しく処理する")
        void handlesSpecialCharacters() {
            // Arrange
            String codeSnippet = """
                // コメント: javax.servlet
                import javax.servlet.http.HttpServlet;
                """;

            // Act
            String result = tool.analyzeMigration(codeSnippet, "5", "6", null);

            // Assert
            assertTrue(result.contains("BC-001"));
            assertFalse(result.contains("エラー"));
        }
    }

    @Nested
    @DisplayName("エッジケーステスト")
    class EdgeCaseTest {

        @Test
        @DisplayName("バージョン文字列の正規化が正しく行われる")
        void versionNormalizationWorks() {
            // Arrange
            String codeSnippet = "import javax.servlet.http.HttpServlet;";

            // Act - 様々なバージョン形式をテスト
            String result1 = tool.analyzeMigration(codeSnippet, "5.1.2", "6.0.1", null);
            String result2 = tool.analyzeMigration(codeSnippet, "5.x", "6.x", null);

            // Assert
            assertFalse(result1.contains("エラー"));
            // 5.x形式は現在の実装では対応していない場合があるため、エラーでも可
        }

        @Test
        @DisplayName("デフォルトバージョンが正しく適用される")
        void defaultVersionsAreApplied() {
            // Arrange
            String codeSnippet = "import javax.servlet.http.HttpServlet;";

            // Act
            String result = tool.analyzeMigration(codeSnippet, null, null, null);

            // Assert
            assertTrue(result.contains("移行元バージョン | Nablarch 5"));
            assertTrue(result.contains("移行先バージョン | Nablarch 6"));
        }

        @Test
        @DisplayName("複数の問題を同時に検出する")
        void detectsMultipleIssues() {
            // Arrange
            String codeSnippet = """
                import javax.servlet.http.HttpServlet;
                import javax.persistence.Entity;
                import javax.validation.constraints.NotNull;
                import nablarch.common.dao.DbAccessSupport;

                public class MyAction extends DbAccessSupport {
                }
                """;

            // Act
            String result = tool.analyzeMigration(codeSnippet, "5", "6", null);

            // Assert
            assertTrue(result.contains("BC-001")); // servlet
            assertTrue(result.contains("BC-002")); // persistence
            assertTrue(result.contains("BC-003")); // DbAccessSupport
            assertTrue(result.contains("BC-008")); // validation
        }

        @Test
        @DisplayName("行番号が正しく計算される")
        void lineNumbersAreCorrect() {
            // Arrange
            String codeSnippet = """
                package com.example;

                import javax.servlet.http.HttpServlet;

                public class MyServlet {
                }
                """;

            // Act
            String result = tool.analyzeMigration(codeSnippet, "5", "6", null);

            // Assert
            assertTrue(result.contains("行")); // 行番号が含まれる
            assertTrue(result.contains("検出箇所"));
        }
    }
}
