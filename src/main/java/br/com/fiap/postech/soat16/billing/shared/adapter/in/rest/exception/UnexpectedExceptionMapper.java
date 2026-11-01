package br.com.fiap.postech.soat16.billing.shared.adapter.in.rest.exception;

import java.util.UUID;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.jboss.logging.Logger;
import org.jboss.logging.MDC;

import br.com.fiap.postech.soat16.billing.shared.adapter.in.rest.RequestCorrelationFilter;
import br.com.fiap.postech.soat16.billing.shared.adapter.in.rest.dto.ApiErrorResponseDto;

/**
 * Fallback para o que nenhum outro mapper trata. Respostas já decididas pelo JAX-RS (401, 403,
 * 404 de rota inexistente, 405) passam intactas; qualquer outra falha vira 500 sem expor a
 * mensagem interna. O {@code traceId} é o X-Request-ID da requisição, o mesmo que já está no
 * MDC do log; só se gera um novo quando o filtro não rodou.
 */
@Provider
public class UnexpectedExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(UnexpectedExceptionMapper.class);
    private static final int INTERNAL_SERVER_ERROR = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(Throwable exception) {
        if (exception instanceof WebApplicationException webApplication) {
            return webApplication.getResponse();
        }

        String traceId = traceId();
        LOG.errorf(exception, "Unexpected error [%s]: %s", traceId, exception.getMessage());

        String path = uriInfo != null ? uriInfo.getPath() : null;
        ApiErrorResponseDto body = ApiErrorResponseDto.of(
                INTERNAL_SERVER_ERROR, null, null, "Internal server error", path, traceId);

        return Response.status(INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }

    private static String traceId() {
        Object requestId = MDC.get(RequestCorrelationFilter.REQUEST_ID_MDC_KEY);
        return requestId != null ? requestId.toString() : UUID.randomUUID().toString();
    }
}
