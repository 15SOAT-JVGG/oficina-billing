package br.com.fiap.postech.soat16.billing.catalog.adapter.in.rest.openapi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.fiap.postech.soat16.billing.shared.test.infrastructure.JwtKeyPairTestResource;
import br.com.fiap.postech.soat16.billing.shared.test.infrastructure.OpenApiContract;
import br.com.fiap.postech.soat16.billing.shared.test.infrastructure.PostgresTestResource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

/**
 * Confere o documento servido em /q/openapi contra a tabela de docs/architecture/rest-contracts.md.
 */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
@QuarkusTestResource(JwtKeyPairTestResource.class)
@DisplayName("OpenAPI — contrato publicado do catálogo de serviços")
class ServiceItemOpenApiContractIT {

    private static final String PATH = "/admin/services";
    private static final String BY_ID = PATH + "/{id}";

    @Test
    @DisplayName("GET /admin/services documenta 200, 401 e 403")
    void listAll() {
        assertEquals(Set.of("200", "401", "403"), OpenApiContract.documentedStatuses("get", PATH));
    }

    @Test
    @DisplayName("GET /admin/services/{id} documenta 200, 401, 403 e 404")
    void findById() {
        assertEquals(Set.of("200", "401", "403", "404"), OpenApiContract.documentedStatuses("get", BY_ID));
    }

    @Test
    @DisplayName("POST /admin/services documenta 201, 400, 401 e 403")
    void create() {
        assertEquals(Set.of("201", "400", "401", "403"), OpenApiContract.documentedStatuses("post", PATH));
    }

    @Test
    @DisplayName("PUT /admin/services/{id} documenta 200, 400, 401, 403 e 404")
    void update() {
        assertEquals(Set.of("200", "400", "401", "403", "404"), OpenApiContract.documentedStatuses("put", BY_ID));
    }

    @Test
    @DisplayName("DELETE /admin/services/{id} documenta 204, 401, 403 e 404")
    void delete() {
        assertEquals(Set.of("204", "401", "403", "404"), OpenApiContract.documentedStatuses("delete", BY_ID));
    }

    @Test
    @DisplayName("toda operação exige bearerAuth")
    void security() {
        for (String[] operation : new String[][] {
                {"get", PATH}, {"post", PATH}, {"get", BY_ID}, {"put", BY_ID}, {"delete", BY_ID}}) {
            assertEquals(true, OpenApiContract.requiresBearerAuth(operation[0], operation[1]),
                    operation[0] + " " + operation[1] + " sem bearerAuth");
        }
    }
}
