package br.com.fiap.postech.soat16.billing.catalog.domain.exception;

import br.com.fiap.postech.soat16.billing.shared.domain.exception.ErrorCode;

public enum ServiceItemErrorCode implements ErrorCode {

    SERVICE_ITEM_NOT_FOUND,
    INVALID_SERVICE_ITEM;

    @Override
    public String getCode() {
        return name();
    }
}
