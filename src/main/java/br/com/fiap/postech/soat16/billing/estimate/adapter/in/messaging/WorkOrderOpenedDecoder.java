package br.com.fiap.postech.soat16.billing.estimate.adapter.in.messaging;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.SpecVersion;

import br.com.fiap.postech.soat16.billing.estimate.application.command.ProcessWorkOrderOpenedCommand;
import br.com.fiap.postech.soat16.billing.estimate.application.command.ProcessWorkOrderOpenedCommand.PartReferenceCommand;
import br.com.fiap.postech.soat16.billing.estimate.application.command.ProcessWorkOrderOpenedCommand.ServiceReferenceCommand;

@ApplicationScoped
public class WorkOrderOpenedDecoder {

    private final ObjectMapper mapper = new ObjectMapper();
    private JsonSchema schema;

    @PostConstruct
    void initialize() {
        var config = new SchemaValidatorsConfig();
        config.setFormatAssertionsEnabled(true);
        var factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        try (InputStream input = getClass().getResourceAsStream("/events/v1/work-order-opened.schema.json")) {
            if (input == null) {
                throw new IllegalStateException("Missing schema: work-order.opened v1");
            }
            schema = factory.getSchema(input, config);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load work-order.opened schema", exception);
        }
    }

    public ProcessWorkOrderOpenedCommand decode(String payload, String traceparent) {
        try {
            JsonNode root = mapper.readTree(payload);
            if (root == null || !schema.validate(root).isEmpty()) {
                throw new IllegalArgumentException("Message does not match work-order.opened v1");
            }
            JsonNode data = root.path("data");
            return new ProcessWorkOrderOpenedCommand(uuid(root, "eventId"), uuid(root, "sagaId"),
                    uuid(root, "correlationId"), uuid(root, "workOrderId"),
                    OffsetDateTime.parse(root.path("occurredAt").asText()), decodeServices(data.path("services")),
                    decodeParts(data.path("parts")), traceparent);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Invalid work-order.opened JSON", exception);
        }
    }

    private List<ServiceReferenceCommand> decodeServices(JsonNode services) {
        List<ServiceReferenceCommand> references = new ArrayList<>();
        services.forEach(item -> references.add(new ServiceReferenceCommand(uuid(item, "serviceItemId"))));
        return references;
    }

    private List<PartReferenceCommand> decodeParts(JsonNode parts) {
        List<PartReferenceCommand> references = new ArrayList<>();
        parts.forEach(item -> references.add(new PartReferenceCommand(uuid(item, "partId"), item.path("quantity").asInt())));
        return references;
    }

    private UUID uuid(JsonNode node, String field) {
        return UUID.fromString(node.path(field).asText());
    }
}
