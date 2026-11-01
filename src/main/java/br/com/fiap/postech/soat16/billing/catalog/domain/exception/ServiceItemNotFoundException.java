package br.com.fiap.postech.soat16.billing.catalog.domain.exception;

import static br.com.fiap.postech.soat16.billing.catalog.domain.exception.ServiceItemErrorCode.SERVICE_ITEM_NOT_FOUND;
import static br.com.fiap.postech.soat16.billing.shared.domain.exception.ErrorType.NOT_FOUND;

import java.util.UUID;

import br.com.fiap.postech.soat16.billing.shared.domain.exception.AppException;

public class ServiceItemNotFoundException extends AppException {

    private static final long serialVersionUID = 1L;

    public ServiceItemNotFoundException(UUID id) {
        super("Service item not found: " + id, SERVICE_ITEM_NOT_FOUND, NOT_FOUND);
    }
}
