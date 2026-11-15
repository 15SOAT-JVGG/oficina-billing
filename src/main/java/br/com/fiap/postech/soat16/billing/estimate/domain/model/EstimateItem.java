package br.com.fiap.postech.soat16.billing.estimate.domain.model;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/** Item imutável com nome e preço capturados do catálogo no instante da abertura da OS. */
public record EstimateItem(UUID catalogItemId, EstimateItemType itemType, String name, int quantity,
        BigDecimal unitPrice) {

    public EstimateItem {
        Objects.requireNonNull(catalogItemId, "Catalog item id is required");
        Objects.requireNonNull(itemType, "Estimate item type is required");
        Objects.requireNonNull(unitPrice, "Unit price is required");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Estimate item name is required");
        }
        if (quantity < 1) {
            throw new IllegalArgumentException("Estimate item quantity must be positive");
        }
        if (unitPrice.signum() <= 0) {
            throw new IllegalArgumentException("Estimate item unit price must be positive");
        }
    }

    public BigDecimal totalPrice() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
