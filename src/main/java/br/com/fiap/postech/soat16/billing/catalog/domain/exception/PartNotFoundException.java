package br.com.fiap.postech.soat16.billing.catalog.domain.exception;

import static br.com.fiap.postech.soat16.billing.catalog.domain.exception.PartErrorCode.PART_NOT_FOUND;
import static br.com.fiap.postech.soat16.billing.shared.domain.exception.ErrorType.NOT_FOUND;

import java.util.UUID;

import br.com.fiap.postech.soat16.billing.shared.domain.exception.AppException;

public class PartNotFoundException extends AppException {

    private static final long serialVersionUID = 1L;

    public PartNotFoundException(UUID id) {
        super("Part not found: " + id, PART_NOT_FOUND, NOT_FOUND);
    }
}
