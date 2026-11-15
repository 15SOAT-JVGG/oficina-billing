package br.com.fiap.postech.soat16.billing.estimate.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("EstimateItem — snapshot de item do orçamento")
class EstimateItemTest {

    @Test
    @DisplayName("calcula o total pelo preço snapshot e quantidade")
    void calculatesLineTotal() {
        var item = new EstimateItem(UUID.randomUUID(), EstimateItemType.PART, "Óleo", 3, new BigDecimal("12.50"));

        assertEquals(new BigDecimal("37.50"), item.totalPrice());
    }

    @Test
    @DisplayName("recusa quantidade nula ou negativa")
    void rejectsNonPositiveQuantity() {
        assertThrows(IllegalArgumentException.class, () -> item(0, new BigDecimal("1.00")));
    }

    @Test
    @DisplayName("recusa preço nulo ou não positivo")
    void rejectsNonPositivePrice() {
        assertThrows(IllegalArgumentException.class, () -> item(1, BigDecimal.ZERO));
        assertThrows(NullPointerException.class, () -> item(1, null));
    }

    @Test
    @DisplayName("recusa nome vazio e identificadores obrigatórios ausentes")
    void rejectsMissingIdentityAndName() {
        assertThrows(IllegalArgumentException.class,
                () -> new EstimateItem(UUID.randomUUID(), EstimateItemType.PART, " ", 1, BigDecimal.ONE));
        assertThrows(NullPointerException.class,
                () -> new EstimateItem(null, EstimateItemType.PART, "Oil", 1, BigDecimal.ONE));
        assertThrows(NullPointerException.class,
                () -> new EstimateItem(UUID.randomUUID(), null, "Oil", 1, BigDecimal.ONE));
    }

    private static EstimateItem item(int quantity, BigDecimal price) {
        return new EstimateItem(UUID.randomUUID(), EstimateItemType.PART, "Oil", quantity, price);
    }
}
