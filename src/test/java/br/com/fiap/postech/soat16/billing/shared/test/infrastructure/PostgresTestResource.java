package br.com.fiap.postech.soat16.billing.shared.test.infrastructure;

import java.io.IOException;
import java.util.Map;

import org.testcontainers.containers.Container;
import org.testcontainers.containers.PostgreSQLContainer;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

/**
 * Sobe um PostgreSQL real, cria o schema do Billing e injeta as URLs reativa e JDBC. O Flyway
 * migra o container na subida do Quarkus com as mesmas migrations do ambiente real.
 */
public class PostgresTestResource implements QuarkusTestResourceLifecycleManager {

    private static final String SCHEMA = "oficina_billing";

    @SuppressWarnings("resource")
    private final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Override
    public Map<String, String> start() {
        postgres.start();
        createSchema();

        int port = postgres.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT);
        String reactiveUrl = "postgresql://%s:%d/%s?search_path=%s".formatted(
                postgres.getHost(), port, postgres.getDatabaseName(), SCHEMA);
        String jdbcUrl = "jdbc:postgresql://%s:%d/%s?currentSchema=%s".formatted(
                postgres.getHost(), port, postgres.getDatabaseName(), SCHEMA);

        return Map.of(
                "quarkus.datasource.reactive.url", reactiveUrl,
                "quarkus.datasource.jdbc.url", jdbcUrl,
                "quarkus.datasource.username", postgres.getUsername(),
                "quarkus.datasource.password", postgres.getPassword());
    }

    private void createSchema() {
        try {
            Container.ExecResult result = postgres.execInContainer(
                    "psql", "-U", postgres.getUsername(), "-d", postgres.getDatabaseName(),
                    "-c", "CREATE SCHEMA IF NOT EXISTS \"%s\"".formatted(SCHEMA));
            if (result.getExitCode() != 0) {
                throw new IllegalStateException("Falha ao criar schema de teste: " + result.getStderr());
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Erro ao criar schema de teste", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrompido ao criar schema de teste", exception);
        }
    }

    @Override
    public void stop() {
        postgres.stop();
    }
}
