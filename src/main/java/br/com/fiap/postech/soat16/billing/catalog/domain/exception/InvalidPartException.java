package br.com.fiap.postech.soat16.billing.catalog.domain.exception;

import static br.com.fiap.postech.soat16.billing.catalog.domain.exception.PartErrorCode.INVALID_PART;
import static br.com.fiap.postech.soat16.billing.shared.domain.exception.ErrorType.VALIDATION;

import br.com.fiap.postech.soat16.billing.shared.domain.exception.AppException;

public class InvalidPartException extends AppException {

    private static final long serialVersionUID = 1L;

    public InvalidPartException(String message) {
        super(message, INVALID_PART, VALIDATION);
    }
}
