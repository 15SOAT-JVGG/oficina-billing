package br.com.fiap.postech.soat16.billing.estimate.adapter.out.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.fiap.postech.soat16.billing.estimate.application.command.EstimateCreationFailureCommand;
import br.com.fiap.postech.soat16.billing.estimate.domain.model.Estimate;
import br.com.fiap.postech.soat16.billing.estimate.domain.model.EstimateItem;
import br.com.fiap.postech.soat16.billing.estimate.domain.model.EstimateItemType;

@DisplayName("EstimateEventEncoder — contratos publicados")
class EstimateEventEncoderTest {

    private EstimateEventEncoder encoder;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        encoder = new EstimateEventEncoder();
        encoder.initialize();
    }

    @Test
    @DisplayName("serializa orçamento com snapshot e soma validada pelo schema")
    void encodesCreatedEvent() throws Exception {
        var serviceId = UUID.randomUUID();
        var partId = UUID.randomUUID();
        var estimate = Estimate.create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), List.of(
                        new EstimateItem(serviceId, EstimateItemType.SERVICE, "Revisão", 1, new BigDecimal("100.00")),
                        new EstimateItem(partId, EstimateItemType.PART, "Filtro", 2, new BigDecimal("12.50"))),
                OffsetDateTime.now());

        var event = mapper.readTree(encoder.encodeCreated(estimate, UUID.randomUUID()));

        assertEquals("estimate.created", event.path("type").asText());
        assertEquals("100.00", event.path("data").path("services").get(0).path("unitPrice").decimalValue()
                .setScale(2).toPlainString());
        assertEquals(0, new BigDecimal("125.00").compareTo(event.path("data").path("totalAmount").decimalValue()));
        assertEquals(2, event.path("data").path("parts").get(0).path("quantity").asInt());
    }

    @Test
    @DisplayName("serializa falha com a causa da abertura")
    void encodesFailedEvent() throws Exception {
        var failure = new EstimateCreationFailureCommand(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), "PART_NOT_FOUND", "Part not found");

        var event = mapper.readTree(encoder.encodeFailed(failure));

        assertEquals("estimate.creation-failed", event.path("type").asText());
        assertEquals(failure.causationId().toString(), event.path("causationId").asText());
        assertEquals("PART_NOT_FOUND", event.path("data").path("code").asText());
        assertTrue(event.path("occurredAt").isTextual());
    }
}
