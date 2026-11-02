package br.com.fiap.postech.soat16.billing.catalog.adapter.in.rest.controller;

import static br.com.fiap.postech.soat16.billing.catalog.adapter.in.rest.openapi.PartControllerDocs.PATH;
import static br.com.fiap.postech.soat16.billing.shared.test.infrastructure.TestTokens.bearer;
import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import br.com.fiap.postech.soat16.billing.shared.test.infrastructure.JwtKeyPairTestResource;
import br.com.fiap.postech.soat16.billing.shared.test.infrastructure.PostgresTestResource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
@QuarkusTestResource(JwtKeyPairTestResource.class)
@DisplayName("PartController — rotas do catálogo de peças com Postgres real")
class PartControllerIT {

    private static final String ADMIN = "ADMIN";
    private static final String MECHANIC = "MECHANIC";
    private static final String AUTHORIZATION = "Authorization";
    private static final String VALIDATION_ERROR = "Validation Error";
    private static final String NOT_FOUND_CODE = "PART_NOT_FOUND";

    private static String uniqueName() {
        return "Filtro de oleo - " + UUID.randomUUID();
    }

    private static String validBody(String name) {
        return """
                {"name":"%s","description":"Filtro do motor","unitPrice":35.90,"unit":"UN","partType":"PART"}
                """.formatted(name);
    }

    private static String create(String name) {
        return given()
                .header(AUTHORIZATION, bearer(ADMIN))
                .contentType(ContentType.JSON)
                .body(validBody(name))
        .when()
                .post(PATH)
        .then()
                .statusCode(201)
                .extract().path("id");
    }

    @Nested
    @DisplayName("POST /admin/parts")
    class Create {

        @Test
        @DisplayName("deve criar a peça e devolver 201 com Location e corpo em OffsetDateTime UTC")
        void shouldCreate() {
            String name = uniqueName();

            ExtractableResponse<Response> created = given()
                    .header(AUTHORIZATION, bearer(ADMIN))
                    .contentType(ContentType.JSON)
                    .body(validBody(name))
            .when()
                    .post(PATH)
            .then()
                    .statusCode(201)
                    .body("id", notNullValue())
                    .body("name", equalTo(name))
                    .body("description", equalTo("Filtro do motor"))
                    .body("unitPrice", equalTo(35.90f))
                    .body("unit", equalTo("UN"))
                    .body("partType", equalTo("PART"))
                    .body("createdAt", matchesPattern(".*(Z|[+-]00:00)$"))
                    .extract();

            String id = created.path("id");
            assertThat(created.header("Location"), endsWith(PATH + "/" + id));
        }

        @Test
        @DisplayName("deve classificar como PART quando partType falta e aceitar SUPPLY explícito")
        void shouldApplyPartTypeDefault() {
            given()
                    .header(AUTHORIZATION, bearer(ADMIN))
                    .contentType(ContentType.JSON)
                    .body("""
                            {"name":"%s","unitPrice":10.00,"unit":"L"}
                            """.formatted(uniqueName()))
            .when()
                    .post(PATH)
            .then()
                    .statusCode(201)
                    .body("partType", equalTo("PART"));

            given()
                    .header(AUTHORIZATION, bearer(ADMIN))
                    .contentType(ContentType.JSON)
                    .body("""
                            {"name":"%s","unitPrice":59.90,"unit":"L","partType":"SUPPLY"}
                            """.formatted("Oleo 5W30 - " + UUID.randomUUID()))
            .when()
                    .post(PATH)
            .then()
                    .statusCode(201)
                    .body("partType", equalTo("SUPPLY"));
        }

        // Clientes do monólito ainda mandam os campos de estoque; eles são ignorados e não
        // voltam na resposta, porque o saldo é do Execution.
        @Test
        @DisplayName("deve ignorar campos de estoque enviados por clientes antigos e não devolvê-los")
        void shouldIgnoreLegacyStockFields() {
            given()
                    .header(AUTHORIZATION, bearer(ADMIN))
                    .contentType(ContentType.JSON)
                    .body("""
                            {"name":"%s","unitPrice":35.90,"unit":"UN","stockQuantity":10,"minimumStock":2}
                            """.formatted(uniqueName()))
            .when()
                    .post(PATH)
            .then()
                    .statusCode(201)
                    .body("$", not(hasKey("stockQuantity")))
                    .body("$", not(hasKey("minimumStock")))
                    .body("$", not(hasKey("lowStock")));
        }

        @Test
        @DisplayName("deve devolver 400 quando faltam nome, preço e unidade")
        void shouldRejectMissingFields() {
            given()
                    .header(AUTHORIZATION, bearer(ADMIN))
                    .contentType(ContentType.JSON)
                    .body("""
                            {"description":"sem nada"}
                            """)
            .when()
                    .post(PATH)
            .then()
                    .statusCode(400)
                    .body("status", equalTo(400))
                    .body("error", equalTo(VALIDATION_ERROR))
                    .body("violations", hasItems(
                            "name: Part name is required", "unit: Unit is required", "unitPrice: Unit price is required"))
                    .body("timestamp", notNullValue());
        }

        @Test
        @DisplayName("deve devolver 400 quando o preço é zero")
        void shouldRejectZeroPrice() {
            given()
                    .header(AUTHORIZATION, bearer(ADMIN))
                    .contentType(ContentType.JSON)
                    .body("""
                            {"name":"%s","unitPrice":0,"unit":"UN"}
                            """.formatted(uniqueName()))
            .when()
                    .post(PATH)
            .then()
                    .statusCode(400)
                    .body("violations", hasItem("unitPrice: Price must be greater than zero"));
        }

        @Test
        @DisplayName("deve devolver 401 sem token")
        void shouldRequireToken() {
            given()
                    .contentType(ContentType.JSON)
                    .body(validBody(uniqueName()))
            .when()
                    .post(PATH)
            .then()
                    .statusCode(401);
        }

        @Test
        @DisplayName("deve devolver 403 para o MECHANIC")
        void shouldForbidMechanic() {
            given()
                    .header(AUTHORIZATION, bearer(MECHANIC))
                    .contentType(ContentType.JSON)
                    .body(validBody(uniqueName()))
            .when()
                    .post(PATH)
            .then()
                    .statusCode(403);
        }
    }

    @Nested
    @DisplayName("GET /admin/parts")
    class ListAll {

        @Test
        @DisplayName("deve listar as peças para o ADMIN")
        void shouldListForAdmin() {
            String id = create(uniqueName());

            given()
                    .header(AUTHORIZATION, bearer(ADMIN))
            .when()
                    .get(PATH)
            .then()
                    .statusCode(200)
                    .body("id", hasItem(id));
        }

        @Test
        @DisplayName("deve listar as peças para o MECHANIC")
        void shouldListForMechanic() {
            create(uniqueName());

            given()
                    .header(AUTHORIZATION, bearer(MECHANIC))
            .when()
                    .get(PATH)
            .then()
                    .statusCode(200)
                    .body("$", not(hasSize(0)));
        }

        @Test
        @DisplayName("deve devolver 401 sem token")
        void shouldRequireToken() {
            given().when().get(PATH).then().statusCode(401);
        }

        @Test
        @DisplayName("deve devolver 403 para um papel desconhecido")
        void shouldForbidUnknownRole() {
            given()
                    .header(AUTHORIZATION, bearer("CUSTOMER"))
            .when()
                    .get(PATH)
            .then()
                    .statusCode(403);
        }

        @Test
        @DisplayName("não deve expor a rota de estoque baixo do monólito")
        void shouldNotServeLowStock() {
            given()
                    .header(AUTHORIZATION, bearer(ADMIN))
            .when()
                    .get(PATH + "/low-stock")
            .then()
                    .statusCode(404);
        }
    }

    @Nested
    @DisplayName("GET /admin/parts/{id}")
    class FindById {

        @Test
        @DisplayName("deve devolver a peça para ADMIN e MECHANIC")
        void shouldFind() {
            String name = uniqueName();
            String id = create(name);

            for (String role : new String[] {ADMIN, MECHANIC}) {
                given()
                        .header(AUTHORIZATION, bearer(role))
                .when()
                        .get(PATH + "/" + id)
                .then()
                        .statusCode(200)
                        .body("id", equalTo(id))
                        .body("name", equalTo(name));
            }
        }

        @Test
        @DisplayName("deve devolver 404 estruturado para id inexistente")
        void shouldReturnNotFound() {
            UUID missing = UUID.randomUUID();

            given()
                    .header(AUTHORIZATION, bearer(ADMIN))
            .when()
                    .get(PATH + "/" + missing)
            .then()
                    .statusCode(404)
                    .body("status", equalTo(404))
                    .body("type", equalTo("NOT_FOUND"))
                    .body("code", equalTo(NOT_FOUND_CODE))
                    .body("message", equalTo("Part not found: " + missing))
                    .body("path", equalTo("/admin/parts/" + missing));
        }

        @Test
        @DisplayName("deve devolver 401 sem token")
        void shouldRequireToken() {
            given().when().get(PATH + "/" + UUID.randomUUID()).then().statusCode(401);
        }

        @Test
        @DisplayName("deve devolver 403 para um papel desconhecido")
        void shouldForbidUnknownRole() {
            given()
                    .header(AUTHORIZATION, bearer("CUSTOMER"))
            .when()
                    .get(PATH + "/" + UUID.randomUUID())
            .then()
                    .statusCode(403);
        }
    }

    @Nested
    @DisplayName("PUT /admin/parts/{id}")
    class Update {

        @Test
        @DisplayName("deve substituir todos os dados e devolver 200")
        void shouldUpdate() {
            String id = create(uniqueName());
            String newName = "Oleo 5W30 - " + UUID.randomUUID();

            given()
                    .header(AUTHORIZATION, bearer(ADMIN))
                    .contentType(ContentType.JSON)
                    .body("""
                            {"name":"%s","description":"Sintetico","unitPrice":59.90,"unit":"L","partType":"SUPPLY"}
                            """.formatted(newName))
            .when()
                    .put(PATH + "/" + id)
            .then()
                    .statusCode(200)
                    .body("id", equalTo(id))
                    .body("name", equalTo(newName))
                    .body("unitPrice", equalTo(59.90f))
                    .body("unit", equalTo("L"))
                    .body("partType", equalTo("SUPPLY"));

            given()
                    .header(AUTHORIZATION, bearer(ADMIN))
            .when()
                    .get(PATH + "/" + id)
            .then()
                    .statusCode(200)
                    .body("partType", equalTo("SUPPLY"));
        }

        @Test
        @DisplayName("deve devolver 400 com as violações do corpo")
        void shouldValidate() {
            String id = create(uniqueName());

            given()
                    .header(AUTHORIZATION, bearer(ADMIN))
                    .contentType(ContentType.JSON)
                    .body("""
                            {"name":"","unitPrice":-1,"unit":""}
                            """)
            .when()
                    .put(PATH + "/" + id)
            .then()
                    .statusCode(400)
                    .body("error", equalTo(VALIDATION_ERROR))
                    .body("violations", hasItems(
                            "name: Part name is required", "unit: Unit is required", "unitPrice: Price must be greater than zero"));
        }

        @Test
        @DisplayName("deve devolver 404 para id inexistente")
        void shouldReturnNotFound() {
            given()
                    .header(AUTHORIZATION, bearer(ADMIN))
                    .contentType(ContentType.JSON)
                    .body(validBody(uniqueName()))
            .when()
                    .put(PATH + "/" + UUID.randomUUID())
            .then()
                    .statusCode(404)
                    .body("type", equalTo("NOT_FOUND"))
                    .body("code", equalTo(NOT_FOUND_CODE));
        }

        @Test
        @DisplayName("deve devolver 401 sem token")
        void shouldRequireToken() {
            given()
                    .contentType(ContentType.JSON)
                    .body(validBody(uniqueName()))
            .when()
                    .put(PATH + "/" + UUID.randomUUID())
            .then()
                    .statusCode(401);
        }

        @Test
        @DisplayName("deve devolver 403 para o MECHANIC")
        void shouldForbidMechanic() {
            String id = create(uniqueName());

            given()
                    .header(AUTHORIZATION, bearer(MECHANIC))
                    .contentType(ContentType.JSON)
                    .body(validBody(uniqueName()))
            .when()
                    .put(PATH + "/" + id)
            .then()
                    .statusCode(403);
        }

        @Test
        @DisplayName("não deve expor o ajuste de estoque do monólito")
        void shouldNotServeStockAdjustment() {
            String id = create(uniqueName());

            given()
                    .header(AUTHORIZATION, bearer(ADMIN))
                    .queryParam("adjustment", 5)
            .when()
                    .patch(PATH + "/" + id + "/stock")
            .then()
                    .statusCode(404);
        }
    }

    @Nested
    @DisplayName("DELETE /admin/parts/{id}")
    class Delete {

        @Test
        @DisplayName("deve remover e devolver 204 sem corpo; a leitura seguinte devolve 404")
        void shouldDelete() {
            String id = create(uniqueName());

            given()
                    .header(AUTHORIZATION, bearer(ADMIN))
            .when()
                    .delete(PATH + "/" + id)
            .then()
                    .statusCode(204)
                    .body(is(""));

            given()
                    .header(AUTHORIZATION, bearer(ADMIN))
            .when()
                    .get(PATH + "/" + id)
            .then()
                    .statusCode(404)
                    .body("code", equalTo(NOT_FOUND_CODE));
        }

        @Test
        @DisplayName("deve devolver 404 para id inexistente")
        void shouldReturnNotFound() {
            given()
                    .header(AUTHORIZATION, bearer(ADMIN))
            .when()
                    .delete(PATH + "/" + UUID.randomUUID())
            .then()
                    .statusCode(404)
                    .body("type", equalTo("NOT_FOUND"))
                    .body("code", equalTo(NOT_FOUND_CODE));
        }

        @Test
        @DisplayName("deve devolver 401 sem token")
        void shouldRequireToken() {
            given().when().delete(PATH + "/" + UUID.randomUUID()).then().statusCode(401);
        }

        @Test
        @DisplayName("deve devolver 403 para o MECHANIC")
        void shouldForbidMechanic() {
            String id = create(uniqueName());

            given()
                    .header(AUTHORIZATION, bearer(MECHANIC))
            .when()
                    .delete(PATH + "/" + id)
            .then()
                    .statusCode(403);
        }
    }
}
