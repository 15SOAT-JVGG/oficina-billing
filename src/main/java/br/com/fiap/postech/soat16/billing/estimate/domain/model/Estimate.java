package br.com.fiap.postech.soat16.billing.estimate.domain.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Orçamento da OS com snapshot dos preços do catálogo pertencente ao Billing. */
public class Estimate {

    private final UUID id;
    private final UUID workOrderId;
    private final UUID openingEventId;
    private final UUID sagaId;
    private final UUID correlationId;
    private final List<EstimateItem> items;
    private final BigDecimal totalAmount;
    private final OffsetDateTime createdAt;

    private Estimate(UUID id, UUID workOrderId, UUID openingEventId, UUID sagaId, UUID correlationId,
            List<EstimateItem> items, BigDecimal totalAmount, OffsetDateTime createdAt) {
        this.id = id;
        this.workOrderId = workOrderId;
        this.openingEventId = openingEventId;
        this.sagaId = sagaId;
        this.correlationId = correlationId;
        this.items = items;
        this.totalAmount = totalAmount;
        this.createdAt = createdAt;
    }

    public static Estimate create(UUID id, UUID workOrderId, UUID openingEventId, UUID sagaId,
            UUID correlationId, List<EstimateItem> items, OffsetDateTime createdAt) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(workOrderId);
        Objects.requireNonNull(openingEventId);
        Objects.requireNonNull(sagaId);
        Objects.requireNonNull(correlationId);
        Objects.requireNonNull(createdAt);
        var snapshot = List.copyOf(items);
        if (snapshot.isEmpty()) {
            throw new IllegalArgumentException("An estimate requires at least one requested item");
        }
        var totalAmount = snapshot.stream().map(EstimateItem::totalPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Estimate(id, workOrderId, openingEventId, sagaId, correlationId, snapshot, totalAmount, createdAt);
    }

    public UUID getId() { return id; }
    public UUID getWorkOrderId() { return workOrderId; }
    public UUID getOpeningEventId() { return openingEventId; }
    public UUID getSagaId() { return sagaId; }
    public UUID getCorrelationId() { return correlationId; }
    public List<EstimateItem> getItems() { return List.copyOf(items); }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
