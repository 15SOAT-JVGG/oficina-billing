package br.com.fiap.postech.soat16.billing.shared.adapter.in.rest.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ValidationExceptionMapper — corpo do 400 igual ao do monólito")
class ValidationExceptionMapperTest {

    @SuppressWarnings("unchecked")
    private static ConstraintViolation<Object> violation(String propertyPath, String message) {
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn(propertyPath);
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn(message);
        return violation;
    }

    @Test
    @DisplayName("deve listar 'campo: mensagem' com o último segmento do caminho, em ordem alfabética")
    @SuppressWarnings("unchecked")
    void shouldListSortedViolations() {
        ConstraintViolationException exception = new ConstraintViolationException(Set.of(
                violation("create.body.name", "Service name is required"),
                violation("create.body.basePrice", "Base price is required"),
                violation("unit", "Unit is required")));

        Response response = new ValidationExceptionMapper().toResponse(exception);
        Map<String, Object> body = (Map<String, Object>) response.getEntity();

        assertEquals(400, response.getStatus());
        assertEquals(400, body.get("status"));
        assertEquals("Validation Error", body.get("error"));
        assertEquals(
                List.of("basePrice: Base price is required", "name: Service name is required", "unit: Unit is required"),
                body.get("violations"));
    }
}
