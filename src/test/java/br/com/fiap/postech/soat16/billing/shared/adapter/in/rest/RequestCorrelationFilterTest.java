package br.com.fiap.postech.soat16.billing.shared.adapter.in.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.MultivaluedHashMap;

import org.jboss.logging.MDC;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("RequestCorrelationFilter — X-Request-ID de ponta a ponta")
class RequestCorrelationFilterTest {

    private final RequestCorrelationFilter filter = new RequestCorrelationFilter();

    @Test
    @DisplayName("deve preservar um request id seguro e devolvê-lo na resposta")
    void shouldPreserveSafeRequestId() {
        ContainerRequestContext request = requestWithProperties();
        ContainerResponseContext response = responseWithHeaders();
        when(request.getHeaderString("X-Request-ID")).thenReturn("client-request-42");

        filter.filter(request);
        assertEquals("client-request-42", MDC.get("requestId"));
        filter.filter(request, response);

        assertEquals("client-request-42", response.getHeaders().getFirst("X-Request-ID"));
        assertNull(MDC.get("requestId"));
    }

    @Test
    @DisplayName("deve substituir um request id inseguro por um gerado")
    void shouldReplaceUnsafeRequestId() {
        ContainerRequestContext request = requestWithProperties();
        ContainerResponseContext response = responseWithHeaders();
        when(request.getHeaderString("X-Request-ID")).thenReturn("token=do-not-echo");

        filter.filter(request);
        String logged = (String) MDC.get("requestId");
        filter.filter(request, response);

        String echoed = (String) response.getHeaders().getFirst("X-Request-ID");
        assertNotNull(echoed);
        assertTrue(echoed.matches("[0-9a-f-]{36}"));
        assertEquals(echoed, logged);
    }

    @Test
    @DisplayName("deve gerar um request id quando o header está ausente")
    void shouldGenerateWhenMissing() {
        ContainerRequestContext request = requestWithProperties();
        ContainerResponseContext response = responseWithHeaders();

        filter.filter(request);
        filter.filter(request, response);

        assertNotNull(response.getHeaders().getFirst("X-Request-ID"));
    }

    private static ContainerRequestContext requestWithProperties() {
        ContainerRequestContext request = mock(ContainerRequestContext.class);
        Map<String, Object> properties = new HashMap<>();
        doAnswer(invocation -> {
            properties.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(request).setProperty(anyString(), org.mockito.ArgumentMatchers.any());
        when(request.getProperty(anyString())).thenAnswer(invocation -> properties.get(invocation.getArgument(0)));
        when(request.getMethod()).thenReturn("GET");
        return request;
    }

    private static ContainerResponseContext responseWithHeaders() {
        ContainerResponseContext response = mock(ContainerResponseContext.class);
        when(response.getHeaders()).thenReturn(new MultivaluedHashMap<>());
        when(response.getStatus()).thenReturn(200);
        return response;
    }
}
