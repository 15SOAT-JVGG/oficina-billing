package br.com.fiap.postech.soat16.billing.catalog.domain.exception;

import br.com.fiap.postech.soat16.billing.shared.domain.exception.ErrorCode;

public enum PartErrorCode implements ErrorCode {

    PART_NOT_FOUND,
    INVALID_PART;

    @Override
    public String getCode() {
        return name();
    }
}
