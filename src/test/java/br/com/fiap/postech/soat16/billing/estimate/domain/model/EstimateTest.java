package br.com.fiap.postech.soat16.billing.estimate.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Estimate — orçamento com snapshot de catálogo")
class EstimateTest {

    @Test
    @DisplayName("soma os itens e mantém uma cópia imutável da lista")
    void calculatesTotalFromItems() {
        var items = List.of(
                new EstimateItem(UUID.randomUUID(), EstimateItemType.SERVICE, "Inspeção", 1, new BigDecimal("90.00")),
                new EstimateItem(UUID.randomUUID(), EstimateItemType.PART, "Filtro", 2, new BigDecimal("15.25")));
        var estimate = create(items);

        assertEquals(new BigDecimal("120.50"), estimate.getTotalAmount());
        assertThrows(UnsupportedOperationException.class, () -> estimate.getItems().clear());
    }

    @Test
    @DisplayName("não cria orçamento vazio")
    void rejectsEmptyEstimate() {
        assertThrows(IllegalArgumentException.class, () -> create(List.of()));
    }

    private static Estimate create(List<EstimateItem> items) {
        return Estimate.create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), items, OffsetDateTime.now());
    }
}
