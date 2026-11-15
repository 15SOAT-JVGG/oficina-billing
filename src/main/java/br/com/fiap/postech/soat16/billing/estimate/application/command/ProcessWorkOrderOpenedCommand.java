package br.com.fiap.postech.soat16.billing.estimate.application.command;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record ProcessWorkOrderOpenedCommand(UUID eventId, UUID sagaId, UUID correlationId, UUID workOrderId,
        OffsetDateTime occurredAt, List<ServiceReferenceCommand> services,
        List<PartReferenceCommand> parts, String traceparent) {

    public ProcessWorkOrderOpenedCommand {
        Objects.requireNonNull(eventId);
        Objects.requireNonNull(sagaId);
        Objects.requireNonNull(correlationId);
        Objects.requireNonNull(workOrderId);
        Objects.requireNonNull(occurredAt);
        services = List.copyOf(services);
        parts = List.copyOf(parts);
    }

    public boolean hasInitialRequest() {
        return !services.isEmpty() || !parts.isEmpty();
    }

    public record ServiceReferenceCommand(UUID serviceItemId) {
        public ServiceReferenceCommand { Objects.requireNonNull(serviceItemId); }
    }

    public record PartReferenceCommand(UUID partId, int quantity) {
        public PartReferenceCommand {
            Objects.requireNonNull(partId);
            if (quantity < 1) {
                throw new IllegalArgumentException("Part quantity must be positive");
            }
        }
    }
}
