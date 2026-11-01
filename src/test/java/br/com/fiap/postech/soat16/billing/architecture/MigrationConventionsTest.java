package br.com.fiap.postech.soat16.billing.architecture;

import static br.com.fiap.postech.soat16.billing.architecture.ArchitectureFixtures.PROJECT_ROOT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * O Flyway é o dono do schema (ADR-0002): as migrations seguem o nome esperado, não referenciam o
 * banco do OS Service e não criam schema (isso é do Flyway, por create-schemas).
 */
@DisplayName("Convenções das migrations Flyway")
class MigrationConventionsTest {

    private static final Path MIGRATIONS = PROJECT_ROOT.resolve("src/main/resources/db/migration");
    private static final Pattern MIGRATION_NAME = Pattern.compile("V(\\d+)__[a-z0-9]+(_[a-z0-9]+)*\\.sql");

    private static List<Path> migrations;

    @BeforeAll
    static void listMigrations() throws IOException {
        try (Stream<Path> files = Files.list(MIGRATIONS)) {
            migrations = files.filter(Files::isRegularFile).sorted().toList();
        }
    }

    @Test
    @DisplayName("toda migration se chama V<n>__snake_case.sql e as versões são únicas")
    void namesFollowThePatternWithUniqueVersions() {
        List<String> versions = migrations.stream()
                .map(path -> {
                    Matcher matcher = MIGRATION_NAME.matcher(path.getFileName().toString());
                    assertTrue(matcher.matches(), "Nome fora do padrão: " + path.getFileName());
                    return matcher.group(1);
                })
                .toList();

        assertEquals(versions.size(), versions.stream().distinct().count(), "Versão de migration duplicada");
    }

    @Test
    @DisplayName("nenhuma migration referencia o schema do OS Service")
    void noReferenceToTheOsSchema() throws IOException {
        for (Path migration : migrations) {
            String content = Files.readString(migration).toLowerCase(Locale.ROOT);
            assertFalse(content.contains("oficina_mecanica"), migration.getFileName() + " referencia oficina_mecanica");
        }
    }

    @Test
    @DisplayName("nenhuma migration cria schema")
    void noCreateSchema() throws IOException {
        for (Path migration : migrations) {
            String content = Files.readString(migration).toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
            assertFalse(content.contains("create schema"), migration.getFileName() + " contém CREATE SCHEMA");
        }
    }
}
