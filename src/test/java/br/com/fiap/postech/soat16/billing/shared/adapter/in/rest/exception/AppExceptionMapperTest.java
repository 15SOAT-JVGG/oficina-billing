package br.com.fiap.postech.soat16.billing.shared.adapter.in.rest.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import br.com.fiap.postech.soat16.billing.shared.adapter.in.rest.dto.ApiErrorResponseDto;
import br.com.fiap.postech.soat16.billing.shared.domain.exception.AppException;
import br.com.fiap.postech.soat16.billing.shared.domain.exception.ErrorType;

@DisplayName("AppExceptionMapper — ErrorType → status HTTP")
class AppExceptionMapperTest {

    private static final class SampleException extends AppException {
        private static final long serialVersionUID = 1L;

        SampleException(ErrorType type) {
            super("sample message", () -> "SAMPLE_CODE", type);
        }
    }

    static Stream<Arguments> statusByType() {
        return Stream.of(
                Arguments.of(ErrorType.VALIDATION, 400),
                Arguments.of(ErrorType.UNAUTHORIZED, 401),
                Arguments.of(ErrorType.NOT_FOUND, 404),
                Arguments.of(ErrorType.CONFLICT, 409),
                Arguments.of(ErrorType.GONE, 410),
                Arguments.of(ErrorType.BUSINESS, 422));
    }

    @ParameterizedTest(name = "{0} → {1}")
    @MethodSource("statusByType")
    @DisplayName("deve traduzir cada tipo no status correspondente com o corpo estruturado")
    void shouldMapTypeToStatus(ErrorType type, int expectedStatus) {
        AppExceptionMapper mapper = new AppExceptionMapper();
        UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getPath()).thenReturn("admin/services/1");
        mapper.uriInfo = uriInfo;

        Response response = mapper.toResponse(new SampleException(type));
        ApiErrorResponseDto body = (ApiErrorResponseDto) response.getEntity();

        assertEquals(expectedStatus, response.getStatus());
        assertEquals(expectedStatus, body.status());
        assertEquals(type, body.type());
        assertEquals("SAMPLE_CODE", body.code());
        assertEquals("sample message", body.message());
        assertEquals("admin/services/1", body.path());
    }

    @Test
    @DisplayName("deve tolerar a ausência de UriInfo")
    void shouldTolerateMissingUriInfo() {
        Response response = new AppExceptionMapper().toResponse(new SampleException(ErrorType.NOT_FOUND));

        assertNull(((ApiErrorResponseDto) response.getEntity()).path());
    }
}
