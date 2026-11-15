package br.com.fiap.postech.soat16.billing.estimate.adapter.out.messaging;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.SpecVersion;

import br.com.fiap.postech.soat16.billing.estimate.application.command.EstimateCreationFailureCommand;
import br.com.fiap.postech.soat16.billing.estimate.domain.model.Estimate;
import br.com.fiap.postech.soat16.billing.estimate.domain.model.EstimateItem;
import br.com.fiap.postech.soat16.billing.estimate.domain.model.EstimateItemType;

@ApplicationScoped
public class EstimateEventEncoder {

    @Inject
    ObjectMapper mapper;
    private JsonSchema createdSchema;
    private JsonSchema failedSchema;

    @PostConstruct
    void initialize() {
        var config = new SchemaValidatorsConfig();
        config.setFormatAssertionsEnabled(true);
        var factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        createdSchema = load(factory, config, "estimate-created");
        failedSchema = load(factory, config, "estimate-creation-failed");
    }

    public String encodeCreated(Estimate estimate, java.util.UUID eventId) {
        ObjectNode root = envelope(eventId, "estimate.created", estimate.getWorkOrderId(), estimate.getSagaId(),
                estimate.getCorrelationId(), estimate.getOpeningEventId());
        ObjectNode data = root.putObject("data");
        data.put("estimateId", estimate.getId().toString());
        var services = data.putArray("services");
        var parts = data.putArray("parts");
        for (EstimateItem item : estimate.getItems()) {
            ObjectNode node = (item.itemType() == EstimateItemType.SERVICE ? services : parts).addObject();
            node.put("catalogItemId", item.catalogItemId().toString());
            node.put("name", item.name());
            node.put("quantity", item.quantity());
            node.put("unitPrice", item.unitPrice());
        }
        data.put("totalAmount", estimate.getTotalAmount());
        return writeAndValidate(root, createdSchema);
    }

    public String encodeFailed(EstimateCreationFailureCommand failure) {
        ObjectNode root = envelope(failure.eventId(), "estimate.creation-failed", failure.workOrderId(),
                failure.sagaId(), failure.correlationId(), failure.causationId());
        ObjectNode data = root.putObject("data");
        data.put("code", failure.code());
        data.put("message", failure.message());
        return writeAndValidate(root, failedSchema);
    }

    private ObjectNode envelope(java.util.UUID eventId, String type, java.util.UUID workOrderId,
            java.util.UUID sagaId, java.util.UUID correlationId, java.util.UUID causationId) {
        ObjectNode root = mapper.createObjectNode();
        root.put("eventId", eventId.toString());
        root.put("type", type);
        root.put("version", 1);
        root.put("sagaId", sagaId.toString());
        root.put("correlationId", correlationId.toString());
        root.put("causationId", causationId.toString());
        root.put("workOrderId", workOrderId.toString());
        root.put("occurredAt", Instant.now().toString());
        return root;
    }

    private String writeAndValidate(JsonNode event, JsonSchema schema) {
        if (!schema.validate(event).isEmpty()) {
            throw new IllegalStateException("Generated Billing event does not match its v1 schema");
        }
        try {
            return mapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize Billing event", exception);
        }
    }

    private JsonSchema load(JsonSchemaFactory factory, SchemaValidatorsConfig config, String name) {
        try (InputStream input = getClass().getResourceAsStream("/events/v1/%s.schema.json".formatted(name))) {
            if (input == null) {
                throw new IllegalStateException("Missing event schema: " + name);
            }
            return factory.getSchema(input, config);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load event schema: " + name, exception);
        }
    }
}
