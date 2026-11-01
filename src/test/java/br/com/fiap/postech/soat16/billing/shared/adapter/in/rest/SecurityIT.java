package br.com.fiap.postech.soat16.billing.shared.adapter.in.rest;

import static br.com.fiap.postech.soat16.billing.shared.test.infrastructure.TestTokens.bearer;
import static br.com.fiap.postech.soat16.billing.shared.test.infrastructure.TestTokens.tokenFromOtherIssuer;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.fiap.postech.soat16.billing.shared.test.infrastructure.JwtKeyPairTestResource;
import br.com.fiap.postech.soat16.billing.shared.test.infrastructure.PostgresTestResource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

/**
 * O Billing só valida tokens: aceita os do issuer do OS Service assinados pela chave do par e
 * recusa qualquer outro. O RBAC fica em @RolesAllowed e não em configuração.
 */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
@QuarkusTestResource(JwtKeyPairTestResource.class)
@DisplayName("Security — validação do JWT RS256 do OS Service")
class SecurityIT {

    private static final String SERVICES_PATH = "/admin/services";
    private static final String AUTHORIZATION = "Authorization";

    @Test
    @DisplayName("deve devolver 401 numa rota administrativa sem token")
    void shouldReturn401WithoutToken() {
        given().when().get(SERVICES_PATH).then().statusCode(401);
    }

    @Test
    @DisplayName("deve devolver 401 para token de outro issuer")
    void shouldReturn401ForAnotherIssuer() {
        given()
                .header(AUTHORIZATION, "Bearer " + tokenFromOtherIssuer("ADMIN"))
        .when()
                .get(SERVICES_PATH)
        .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("deve devolver 401 para token malformado")
    void shouldReturn401ForMalformedToken() {
        given()
                .header(AUTHORIZATION, "Bearer nao-e-um-jwt")
        .when()
                .get(SERVICES_PATH)
        .then()
                .statusCode(401);
    }

    @Test
    @DisplayName("deve liberar a leitura ao MECHANIC")
    void shouldAllowMechanicToRead() {
        given()
                .header(AUTHORIZATION, bearer("MECHANIC"))
        .when()
                .get(SERVICES_PATH)
        .then()
                .statusCode(200);
    }

    @Test
    @DisplayName("deve devolver 403 ao MECHANIC na escrita")
    void shouldForbidMechanicToWrite() {
        given()
                .header(AUTHORIZATION, bearer("MECHANIC"))
                .contentType(ContentType.JSON)
                .body("""
                        {"name":"Troca de oleo","basePrice":80.00}
                        """)
        .when()
                .post(SERVICES_PATH)
        .then()
                .statusCode(403);
    }

    @Test
    @DisplayName("deve liberar a escrita ao ADMIN e propagar o X-Request-ID")
    void shouldAllowAdminToWrite() {
        given()
                .header(AUTHORIZATION, bearer("ADMIN"))
                .header("X-Request-ID", "security-it-42")
                .contentType(ContentType.JSON)
                .body("""
                        {"name":"Troca de oleo","basePrice":80.00}
                        """)
        .when()
                .post(SERVICES_PATH)
        .then()
                .statusCode(201)
                .header("X-Request-ID", equalTo("security-it-42"));
    }

    @Test
    @DisplayName("deve servir o OpenAPI sem token")
    void shouldServeOpenApiWithoutToken() {
        given().when().get("/q/openapi").then().statusCode(200).body(notNullValue());
    }
}
