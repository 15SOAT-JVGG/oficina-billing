package br.com.fiap.postech.soat16.billing.shared.domain.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AppException — código e tipo estáveis")
class AppExceptionTest {

    private enum TestErrorCode implements ErrorCode {
        SOMETHING_WRONG;

        @Override
        public String getCode() {
            return name();
        }
    }

    private static final class TestException extends AppException {
        private static final long serialVersionUID = 1L;

        TestException(String message) {
            super(message, TestErrorCode.SOMETHING_WRONG, ErrorType.BUSINESS);
        }

        TestException(String message, Throwable cause) {
            super(message, TestErrorCode.SOMETHING_WRONG, ErrorType.CONFLICT, cause);
        }
    }

    @Test
    @DisplayName("deve expor mensagem, código e tipo")
    void shouldExposeCodeAndType() {
        TestException exception = new TestException("boom");

        assertEquals("boom", exception.getMessage());
        assertEquals("SOMETHING_WRONG", exception.getCode());
        assertEquals(ErrorType.BUSINESS, exception.getType());
    }

    @Test
    @DisplayName("deve preservar a causa quando informada")
    void shouldKeepCause() {
        IllegalStateException cause = new IllegalStateException("infra");

        TestException exception = new TestException("boom", cause);

        assertSame(cause, exception.getCause());
        assertEquals(ErrorType.CONFLICT, exception.getType());
    }
}
