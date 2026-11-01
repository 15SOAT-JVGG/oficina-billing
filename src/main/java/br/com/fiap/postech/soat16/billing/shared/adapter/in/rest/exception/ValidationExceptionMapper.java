package br.com.fiap.postech.soat16.billing.shared.adapter.in.rest.exception;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Corpo do 400 de Bean Validation, idêntico ao do monólito: é parte do contrato das rotas do
 * catálogo (docs/architecture/rest-contracts.md).
 */
@Provider
public class ValidationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        List<String> violations = exception.getConstraintViolations().stream()
                .map(violation -> {
                    String field = violation.getPropertyPath().toString();
                    if (field.contains(".")) {
                        field = field.substring(field.lastIndexOf('.') + 1);
                    }
                    return field + ": " + violation.getMessage();
                })
                .sorted()
                .toList();

        return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of(
                        "status", 400,
                        "error", "Validation Error",
                        "violations", violations,
                        "timestamp", OffsetDateTime.now().toString()))
                .build();
    }
}
