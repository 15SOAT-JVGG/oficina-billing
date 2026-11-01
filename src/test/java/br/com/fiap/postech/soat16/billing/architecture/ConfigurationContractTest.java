package br.com.fiap.postech.soat16.billing.architecture;

import static br.com.fiap.postech.soat16.billing.architecture.ArchitectureFixtures.PROJECT_ROOT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

/**
 * Lê o application.yaml sem subir o Quarkus e afirma as chaves que são decisão arquitetural.
 */
@DisplayName("Contrato do application.yaml")
class ConfigurationContractTest {

    private static final Path APPLICATION_YAML = PROJECT_ROOT.resolve("src/main/resources/application.yaml");
    private static final String SCHEMA = "oficina_billing";

    private static Map<String, Object> configuration;

    @BeforeAll
    static void loadConfiguration() throws IOException {
        try (InputStream input = Files.newInputStream(APPLICATION_YAML)) {
            configuration = new Yaml().load(input);
        }
    }

    @SuppressWarnings("unchecked")
    private static Object value(String dottedPath) {
        Object current = configuration;
        for (String segment : dottedPath.split("\\.")) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = ((Map<String, Object>) map).get(segment);
        }
        return current;
    }

    private static String text(String dottedPath) {
        Object found = value(dottedPath);
        assertNotNull(found, dottedPath + " ausente");
        return String.valueOf(found);
    }

    @Nested
    @DisplayName("Persistência (ADR-0002)")
    class Persistence {

        @Test
        @DisplayName("o Hibernate só valida o schema")
        void hibernateValidates() {
            assertEquals("validate", text("quarkus.hibernate-orm.schema-management.strategy"));
        }

        @Test
        @DisplayName("o Flyway migra na inicialização")
        void flywayMigratesAtStart() {
            assertEquals("true", text("quarkus.flyway.migrate-at-start"));
        }

        @Test
        @DisplayName("o schema é oficina_billing no Flyway e no Hibernate")
        void ownSchema() {
            assertEquals(SCHEMA, text("quarkus.flyway.schemas"));
            assertEquals(SCHEMA, text("quarkus.hibernate-orm.database.default-schema"));
        }

        @Test
        @DisplayName("as URLs JDBC e reativa fixam o schema da conexão")
        void urlsPinTheSchema() {
            assertTrue(text("quarkus.datasource.jdbc.url").contains("currentSchema=" + SCHEMA));
            assertTrue(text("quarkus.datasource.reactive.url").contains("search_path=" + SCHEMA));
        }

        @Test
        @DisplayName("o perfil de teste não troca a estratégia de schema nem liga DevServices")
        void testProfileDoesNotTouchSchema() {
            assertNull(value("%test.quarkus.hibernate-orm.schema-management"));
            assertNull(value("%test.quarkus.datasource.devservices"));
        }

        @Test
        @DisplayName("DevServices ficam desligados")
        void devServicesDisabled() {
            assertEquals("false", text("quarkus.devservices.enabled"));
        }
    }

    @Nested
    @DisplayName("Segurança")
    class Security {

        @Test
        @DisplayName("nenhuma rota nasce aberta por omissão")
        void denyUnannotatedEndpoints() {
            assertEquals("true", text("quarkus.security.jaxrs.deny-unannotated-endpoints"));
        }

        @Test
        @DisplayName("valida com a chave pública e o issuer do OS Service")
        void verifiesWithTheOsPublicKey() {
            assertNotNull(value("mp.jwt.verify.publickey.location"));
            assertEquals("${JWT_ISSUER:oficina-api}", text("mp.jwt.verify.issuer"));
        }

        @Test
        @DisplayName("o Billing não emite token")
        void doesNotSignTokens() {
            assertNull(value("smallrye.jwt.sign"));
            assertNull(value("smallrye.jwt.new-token"));
        }
    }

    @Nested
    @DisplayName("Observabilidade")
    class Observability {

        @Test
        @DisplayName("health e métricas ficam na porta 9000")
        void managementPort() {
            assertEquals("true", text("quarkus.management.enabled"));
            assertEquals("9000", text("quarkus.management.port"));
        }

        @Test
        @DisplayName("logs são estruturados em JSON")
        void jsonLogs() {
            assertEquals("true", text("quarkus.log.console.json.enabled"));
        }
    }
}
