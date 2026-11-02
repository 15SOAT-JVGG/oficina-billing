package br.com.fiap.postech.soat16.billing.catalog.application.result;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import br.com.fiap.postech.soat16.billing.catalog.domain.model.Part;
import br.com.fiap.postech.soat16.billing.catalog.domain.model.PartType;

public record PartResult(
        UUID id,
        String name,
        String description,
        BigDecimal unitPrice,
        String unit,
        PartType partType,
        OffsetDateTime createdAt
) {

    public static PartResult from(Part part) {
        return new PartResult(
                part.getId(),
                part.getName(),
                part.getDescription(),
                part.getUnitPrice(),
                part.getUnit(),
                part.getPartType(),
                part.getCreatedAt());
    }
}
