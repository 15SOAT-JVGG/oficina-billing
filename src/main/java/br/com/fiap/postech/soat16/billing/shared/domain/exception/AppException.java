package br.com.fiap.postech.soat16.billing.shared.domain.exception;

/**
 * Base de toda exceção de negócio do Billing (ADR-0003): carrega o código estável que o cliente
 * lê e o tipo que a borda REST traduz em status HTTP.
 */
public abstract class AppException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ErrorType type;
    private final String code;

    protected AppException(String message, ErrorCode errorCode, ErrorType errorType) {
        super(message);
        this.type = errorType;
        this.code = errorCode.getCode();
    }

    protected AppException(String message, ErrorCode errorCode, ErrorType errorType, Throwable cause) {
        super(message, cause);
        this.type = errorType;
        this.code = errorCode.getCode();
    }

    public ErrorType getType() {
        return type;
    }

    public String getCode() {
        return code;
    }
}
