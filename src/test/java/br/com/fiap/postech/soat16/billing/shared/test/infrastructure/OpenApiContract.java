package br.com.fiap.postech.soat16.billing.shared.test.infrastructure;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import java.util.Map;
import java.util.Set;

import io.restassured.path.json.JsonPath;

/**
 * Leitura do documento OpenAPI servido pela aplicação de teste. Só pode ser buscado depois que o
 * Quarkus publica a porta, por isso é carregado sob demanda e não num @BeforeAll.
 */
public final class OpenApiContract {

    private static JsonPath document;

    private OpenApiContract() {
    }

    private static synchronized JsonPath document() {
        if (document == null) {
            document = given()
                    .queryParam("format", "json")
            .when()
                    .get("/q/openapi")
            .then()
                    .statusCode(200)
                    .extract().jsonPath();
        }
        return document;
    }

    private static Map<String, Object> operation(String method, String path) {
        Map<String, Object> operation = document().getMap("paths.'" + path + "'." + method);
        assertNotNull(operation, method.toUpperCase() + " " + path + " não está documentada");
        return operation;
    }

    @SuppressWarnings("unchecked")
    public static Set<String> documentedStatuses(String method, String path) {
        return Set.copyOf(((Map<String, Object>) operation(method, path).get("responses")).keySet());
    }

    @SuppressWarnings("unchecked")
    public static boolean requiresBearerAuth(String method, String path) {
        List<Map<String, Object>> security = (List<Map<String, Object>>) operation(method, path).get("security");
        return security != null && security.stream().anyMatch(requirement -> requirement.containsKey("bearerAuth"));
    }

    public static boolean hasPath(String path) {
        return document().getMap("paths.'" + path + "'") != null;
    }
}
