package br.com.fiap.postech.soat16.billing.catalog.domain.exception;

import static br.com.fiap.postech.soat16.billing.catalog.domain.exception.ServiceItemErrorCode.INVALID_SERVICE_ITEM;
import static br.com.fiap.postech.soat16.billing.shared.domain.exception.ErrorType.VALIDATION;

import br.com.fiap.postech.soat16.billing.shared.domain.exception.AppException;

public class InvalidServiceItemException extends AppException {

    private static final long serialVersionUID = 1L;

    public InvalidServiceItemException(String message) {
        super(message, INVALID_SERVICE_ITEM, VALIDATION);
    }
}
