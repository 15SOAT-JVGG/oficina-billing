package br.com.fiap.postech.soat16.billing.shared.adapter.in.rest;

import java.util.UUID;
import java.util.regex.Pattern;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

import org.jboss.logging.Logger;
import org.jboss.logging.MDC;

import io.opentelemetry.api.trace.Span;

/**
 * Propaga um identificador de correlação limitado para o header de resposta, o MDC do log JSON e
 * o trace ativo. O Quarkus guarda o MDC no contexto reativo duplicado, então as continuações do
 * Mutiny mantêm o valor sem ThreadLocal manual.
 */
@Provider
@Priority(Priorities.AUTHENTICATION - 1)
public class RequestCorrelationFilter implements ContainerRequestFilter, ContainerResponseFilter {

    static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String REQUEST_ID_PROPERTY = RequestCorrelationFilter.class.getName() + ".requestId";
    private static final Pattern SAFE_REQUEST_ID = Pattern.compile("[A-Za-z0-9._:-]{1,128}");
    private static final Logger LOG = Logger.getLogger(RequestCorrelationFilter.class);

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String requestId = validRequestId(requestContext.getHeaderString(REQUEST_ID_HEADER));
        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
        }

        requestContext.setProperty(REQUEST_ID_PROPERTY, requestId);
        MDC.put("requestId", requestId);
        Span.current().setAttribute("request.id", requestId);
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        Object requestId = requestContext.getProperty(REQUEST_ID_PROPERTY);
        if (requestId instanceof String value) {
            responseContext.getHeaders().putSingle(REQUEST_ID_HEADER, value);
        }
        LOG.infof("HTTP completed method=%s status=%d", requestContext.getMethod(), responseContext.getStatus());
        MDC.remove("requestId");
    }

    private static String validRequestId(String requestId) {
        return requestId != null && SAFE_REQUEST_ID.matcher(requestId).matches() ? requestId : null;
    }
}
