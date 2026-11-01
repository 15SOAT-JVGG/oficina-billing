package br.com.fiap.postech.soat16.billing.shared.adapter.in.rest.exception;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import br.com.fiap.postech.soat16.billing.shared.adapter.in.rest.dto.ApiErrorResponseDto;
import br.com.fiap.postech.soat16.billing.shared.domain.exception.AppException;

/**
 * Único ponto onde {@code ErrorType} vira status HTTP (ADR-0003). Erro novo é exceção nova com
 * código novo; este mapper não muda.
 */
@Provider
public class AppExceptionMapper implements ExceptionMapper<AppException> {

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(AppException exception) {
        int status = switch (exception.getType()) {
            case VALIDATION -> 400;
            case UNAUTHORIZED -> 401;
            case NOT_FOUND -> 404;
            case CONFLICT -> 409;
            case GONE -> 410;
            case BUSINESS -> 422;
        };

        String path = uriInfo != null ? uriInfo.getPath() : null;
        ApiErrorResponseDto body = ApiErrorResponseDto.of(
                status, exception.getType(), exception.getCode(), exception.getMessage(), path, null);

        return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }
}
