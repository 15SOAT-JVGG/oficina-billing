package br.com.fiap.postech.soat16.billing.shared.adapter.in.rest.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.fiap.postech.soat16.billing.shared.adapter.in.rest.dto.ApiErrorResponseDto;

@DisplayName("UnexpectedExceptionMapper — fallback para 500")
class UnexpectedExceptionMapperTest {

    @Test
    @DisplayName("deve devolver 500 com traceId e sem expor a mensagem interna")
    void shouldHideInternalMessage() {
        UnexpectedExceptionMapper mapper = new UnexpectedExceptionMapper();
        UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getPath()).thenReturn("admin/parts");
        mapper.uriInfo = uriInfo;

        Response response = mapper.toResponse(new IllegalStateException("segredo interno"));
        ApiErrorResponseDto body = (ApiErrorResponseDto) response.getEntity();

        assertEquals(500, response.getStatus());
        assertEquals(500, body.status());
        assertEquals("Internal server error", body.message());
        assertNull(body.type());
        assertNotNull(body.traceId());
        assertEquals("admin/parts", body.path());
    }

    @Test
    @DisplayName("deve preservar a resposta de uma WebApplicationException")
    void shouldPassThroughWebApplicationException() {
        NotFoundException notFound = new NotFoundException();

        Response response = new UnexpectedExceptionMapper().toResponse(notFound);

        assertSame(notFound.getResponse(), response);
        assertEquals(404, response.getStatus());
    }
}
