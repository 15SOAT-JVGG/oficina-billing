package br.com.fiap.postech.soat16.billing.catalog.application.result;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import br.com.fiap.postech.soat16.billing.catalog.domain.model.ServiceItem;

public record ServiceItemResult(
        UUID id,
        String name,
        String description,
        BigDecimal basePrice,
        Integer estimatedDurationMinutes,
        boolean active,
        OffsetDateTime createdAt
) {

    public static ServiceItemResult from(ServiceItem serviceItem) {
        return new ServiceItemResult(
                serviceItem.getId(),
                serviceItem.getName(),
                serviceItem.getDescription(),
                serviceItem.getBasePrice(),
                serviceItem.getEstimatedDurationMinutes(),
                serviceItem.isActive(),
                serviceItem.getCreatedAt());
    }
}
