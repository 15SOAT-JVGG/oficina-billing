package br.com.fiap.postech.soat16.billing.catalog.adapter.in.rest.controller;

import static br.com.fiap.postech.soat16.billing.catalog.adapter.in.rest.controller.ServiceItemController.SERVICES_PATH;
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
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

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
@DisplayName("ServiceItemController — rotas do catálogo de serviços com Postgres real")
class ServiceItemControllerIT {

    private static final String ADMIN = "ADMIN";
    private static final String MECHANIC = "MECHANIC";
    private static final String AUTHORIZATION = "Authorization";
    private static final String VALIDATION_ERROR = "Validation Error";
    private static final String NOT_FOUND_CODE = "SERVICE_ITEM_NOT_FOUND";

    private static String uniqueName() {
        return "Troca de oleo - " + UUID.randomUUID();
    }

    private static String body(String name, String basePrice, String extra) {
        return """
                {"name":%s,"description":"Troca de oleo e filtro","basePrice":%s,"estimatedDurationMinutes":30%s}
                """.formatted(name == null ? "null" : "\"" + name + "\"", basePrice, extra);
    }

    private static String validBody(String name) {
        return body(name, "80.00", ",\"active\":true");
    }

    private static String create(String name) {
        return given()
                .header(AUTHORIZATION, bearer(ADMIN))
                .contentType(ContentType.JSON)
                .body(validBody(name))
        .when()
                .post(SERVICES_PATH)
        .then()
                .statusCode(201)
                .extract().path("id");
    }

    @Nested
    @DisplayName("POST /admin/services")
    class Create {

        @Test
        @DisplayName("deve criar o serviço e devolver 201 com Location e corpo")
        void shouldCreate() {
            String name = uniqueName();

            ExtractableResponse<Response> created = given()
                    .header(AUTHORIZATION, bearer(ADMIN))
                    .contentType(ContentType.JSON)
                    .body(validBody(name))
            .when()
                    .post(SERVICES_PATH)
            .then()
                    .statusCode(201)
                    .body("id", notNullValue())
                    .body("name", equalTo(name))
                    .body("description", equalTo("Troca de oleo e filtro"))
                    .body("basePrice", equalTo(80.00f))
                    .body("estimatedDurationMinutes", equalTo(30))
                    .body("active", is(true))
                    .body("createdAt", notNullValue())
                    .extract();

            String id = created.path("id");
            assertThat(created.header("Location"), endsWith(SERVICES_PATH + "/" + id));
        }

        @Test
        @DisplayName("deve considerar ativo e sem descrição quando os campos opcionais faltam")
        void shouldApplyDefaults() {
            given()
                    .header(AUTHORIZATION, bearer(ADMIN))
                    .contentType(ContentType.JSON)
                    .body("""
                            {"name":"%s","basePrice":50.00}
                            """.formatted(uniqueName()))
            .when()
                    .post(SERVICES_PATH)
            .then()
                    .statusCode(201)
                    .body("active", is(true))
                    .body("estimatedDurationMinutes", nullValue())
                    .body("$", not(hasKey("description")));
        }

        @Test
        @DisplayName("deve devolver 400 quando faltam nome e preço")
        void shouldRejectMissingNameAndPrice() {
            given()
                    .header(AUTHORIZATION, bearer(ADMIN))
                    .contentType(ContentType.JSON)
                    .body("""
                            {"description":"sem nome"}
                            """)
            .when()
                    .post(SERVICES_PATH)
            .then()
                    .statusCode(400)
                    .body("status", equalTo(400))
                    .body("error", equalTo(VALIDATION_ERROR))
                    .body("violations", hasItems("basePrice: Base price is required", "name: Service name is required"))
                    .body("timestamp", notNullValue());
        }

        @Test
        @DisplayName("deve devolver 400 quando o preço é zero")
        void shouldRejectZeroPrice() {
            given()
                    .header(AUTHORIZATION, bearer(ADMIN))
                    .contentType(ContentType.JSON)
                    .body(body(uniqueName(), "0.00", ""))
            .when()
                    .post(SERVICES_PATH)
            .then()
                    .statusCode(400)
                    .body("error", equalTo(VALIDATION_ERROR))
                    .body("violations", hasItem("basePrice: Base price must be greater than zero"));
        }

        @Test
        @DisplayName("deve devolver 400 quando a duração é negativa")
        void shouldRejectNegativeDuration() {
            given()
                    .header(AUTHORIZATION, bearer(ADMIN))
                    .contentType(ContentType.JSON)
                    .body("""
                            {"name":"%s","basePrice":80.00,"estimatedDurationMinutes":-1}
                            """.formatted(uniqueName()))
            .when()
                    .post(SERVICES_PATH)
            .then()
                    .statusCode(400)
                    .body("violations", hasItem("estimatedDurationMinutes: Estimated duration cannot be negative"));
        }

        @Test
        @DisplayName("deve devolver 401 sem token")
        void shouldRequireToken() {
            given()
                    .contentType(ContentType.JSON)
                    .body(validBody(uniqueName()))
            .when()
                    .post(SERVICES_PATH)
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
                    .post(SERVICES_PATH)
            .then()
                    .statusCode(403);
        }
    }

    @Nested
    @DisplayName("GET /admin/services")
    class ListAll {

        @Test
        @DisplayName("deve listar os serviços para o ADMIN")
        void shouldListForAdmin() {
            String id = create(uniqueName());

            given()
                    .header(AUTHORIZATION, bearer(ADMIN))
            .when()
                    .get(SERVICES_PATH)
            .then()
                    .statusCode(200)
                    .body("id", hasItem(id));
        }

        @Test
        @DisplayName("deve listar os serviços para o MECHANIC")
        void shouldListForMechanic() {
            create(uniqueName());

            given()
                    .header(AUTHORIZATION, bearer(MECHANIC))
            .when()
                    .get(SERVICES_PATH)
            .then()
                    .statusCode(200)
                    .body("$", not(hasSize(0)));
        }

        @Test
        @DisplayName("deve devolver 401 sem token")
        void shouldRequireToken() {
            given().when().get(SERVICES_PATH).then().statusCode(401);
        }

        @Test
        @DisplayName("deve devolver 403 para um papel desconhecido")
        void shouldForbidUnknownRole() {
            given()
                    .header(AUTHORIZATION, bearer("CUSTOMER"))
            .when()
                    .get(SERVICES_PATH)
            .then()
                    .statusCode(403);
        }
    }

    @Nested
    @DisplayName("GET /admin/services/{id}")
    class FindById {

        @Test
        @DisplayName("deve devolver o serviço para ADMIN e MECHANIC")
        void shouldFind() {
            String name = uniqueName();
            String id = create(name);

            for (String role : new String[] {ADMIN, MECHANIC}) {
                given()
                        .header(AUTHORIZATION, bearer(role))
                .when()
                        .get(SERVICES_PATH + "/" + id)
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
                    .get(SERVICES_PATH + "/" + missing)
            .then()
                    .statusCode(404)
                    .body("status", equalTo(404))
                    .body("type", equalTo("NOT_FOUND"))
                    .body("code", equalTo(NOT_FOUND_CODE))
                    .body("message", equalTo("Service item not found: " + missing))
                    .body("path", equalTo("/admin/services/" + missing));
        }

        @Test
        @DisplayName("deve devolver 401 sem token")
        void shouldRequireToken() {
            given().when().get(SERVICES_PATH + "/" + UUID.randomUUID()).then().statusCode(401);
        }

        @Test
        @DisplayName("deve devolver 403 para um papel desconhecido")
        void shouldForbidUnknownRole() {
            given()
                    .header(AUTHORIZATION, bearer("CUSTOMER"))
            .when()
                    .get(SERVICES_PATH + "/" + UUID.randomUUID())
            .then()
                    .statusCode(403);
        }
    }

    @Nested
    @DisplayName("PUT /admin/services/{id}")
    class Update {

        @Test
        @DisplayName("deve substituir todos os dados e devolver 200")
        void shouldUpdate() {
            String id = create(uniqueName());
            String newName = "Alinhamento - " + UUID.randomUUID();

            given()
                    .header(AUTHORIZATION, bearer(ADMIN))
                    .contentType(ContentType.JSON)
                    .body("""
                            {"name":"%s","basePrice":120.00,"estimatedDurationMinutes":45,"active":false}
                            """.formatted(newName))
            .when()
                    .put(SERVICES_PATH + "/" + id)
            .then()
                    .statusCode(200)
                    .body("id", equalTo(id))
                    .body("name", equalTo(newName))
                    .body("basePrice", equalTo(120.00f))
                    .body("estimatedDurationMinutes", equalTo(45))
                    .body("active", is(false));

            given()
                    .header(AUTHORIZATION, bearer(ADMIN))
            .when()
                    .get(SERVICES_PATH + "/" + id)
            .then()
                    .statusCode(200)
                    .body("name", equalTo(newName))
                    .body("active", is(false));
        }

        @Test
        @DisplayName("deve devolver 400 com as violações do corpo")
        void shouldValidate() {
            String id = create(uniqueName());

            given()
                    .header(AUTHORIZATION, bearer(ADMIN))
                    .contentType(ContentType.JSON)
                    .body("""
                            {"name":"","basePrice":-5}
                            """)
            .when()
                    .put(SERVICES_PATH + "/" + id)
            .then()
                    .statusCode(400)
                    .body("error", equalTo(VALIDATION_ERROR))
                    .body("violations", hasItems(
                            "basePrice: Base price must be greater than zero", "name: Service name is required"));
        }

        @Test
        @DisplayName("deve devolver 404 para id inexistente")
        void shouldReturnNotFound() {
            given()
                    .header(AUTHORIZATION, bearer(ADMIN))
                    .contentType(ContentType.JSON)
                    .body(validBody(uniqueName()))
            .when()
                    .put(SERVICES_PATH + "/" + UUID.randomUUID())
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
                    .put(SERVICES_PATH + "/" + UUID.randomUUID())
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
                    .put(SERVICES_PATH + "/" + id)
            .then()
                    .statusCode(403);
        }
    }

    @Nested
    @DisplayName("DELETE /admin/services/{id}")
    class Delete {

        @Test
        @DisplayName("deve remover e devolver 204 sem corpo; a leitura seguinte devolve 404")
        void shouldDelete() {
            String id = create(uniqueName());

            given()
                    .header(AUTHORIZATION, bearer(ADMIN))
            .when()
                    .delete(SERVICES_PATH + "/" + id)
            .then()
                    .statusCode(204)
                    .body(is(""));

            given()
                    .header(AUTHORIZATION, bearer(ADMIN))
            .when()
                    .get(SERVICES_PATH + "/" + id)
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
                    .delete(SERVICES_PATH + "/" + UUID.randomUUID())
            .then()
                    .statusCode(404)
                    .body("type", equalTo("NOT_FOUND"))
                    .body("code", equalTo(NOT_FOUND_CODE));
        }

        @Test
        @DisplayName("deve devolver 401 sem token")
        void shouldRequireToken() {
            given().when().delete(SERVICES_PATH + "/" + UUID.randomUUID()).then().statusCode(401);
        }

        @Test
        @DisplayName("deve devolver 403 para o MECHANIC")
        void shouldForbidMechanic() {
            String id = create(uniqueName());

            given()
                    .header(AUTHORIZATION, bearer(MECHANIC))
            .when()
                    .delete(SERVICES_PATH + "/" + id)
            .then()
                    .statusCode(403);
        }
    }
}
