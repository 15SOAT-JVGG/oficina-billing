package br.com.fiap.postech.soat16.billing.estimate.adapter.out.persistence;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import br.com.fiap.postech.soat16.billing.estimate.adapter.out.messaging.EstimateEventEncoder;
import br.com.fiap.postech.soat16.billing.estimate.application.command.EstimateCreationFailureCommand;
import br.com.fiap.postech.soat16.billing.estimate.application.port.out.EventInboxPort;
import br.com.fiap.postech.soat16.billing.estimate.application.port.out.EventOutboxPort;
import br.com.fiap.postech.soat16.billing.estimate.domain.model.Estimate;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.mutiny.Uni;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
public class BillingEventJournalAdapter implements EventInboxPort, EventOutboxPort {

    private final EstimateEventEncoder encoder;

    @Override
    public Uni<Boolean> claim(UUID eventId, UUID workOrderId, String eventType) {
        return Panache.getSession().flatMap(session -> session.createNativeQuery("""
                INSERT INTO oficina_billing.billing_event_inbox (event_id, work_order_id, event_type, received_at)
                VALUES (:eventId, :workOrderId, :eventType, now())
                ON CONFLICT (event_id) DO NOTHING
                """)
                .setParameter("eventId", eventId)
                .setParameter("workOrderId", workOrderId)
                .setParameter("eventType", eventType)
                .executeUpdate()
                .map(inserted -> inserted == 1));
    }

    @Override
    public Uni<Void> recordCreated(Estimate estimate, String traceparent) {
        UUID eventId = UUID.randomUUID();
        return insertOutbox(eventId, estimate.getWorkOrderId(), "estimate.created",
                encoder.encodeCreated(estimate, eventId), traceparent);
    }

    @Override
    public Uni<Void> recordFailed(EstimateCreationFailureCommand failure, String traceparent) {
        return insertOutbox(failure.eventId(), failure.workOrderId(), "estimate.creation-failed",
                encoder.encodeFailed(failure), traceparent);
    }

    private Uni<Void> insertOutbox(UUID eventId, UUID workOrderId, String eventType, String payload,
            String traceparent) {
        return Panache.getSession().flatMap(session -> session.createNativeQuery("""
                INSERT INTO oficina_billing.billing_event_outbox
                    (event_id, work_order_id, event_type, payload, traceparent, created_at)
                VALUES (:eventId, :workOrderId, :eventType, CAST(CAST(:payload AS text) AS jsonb), :traceparent, now())
                """)
                .setParameter("eventId", eventId)
                .setParameter("workOrderId", workOrderId)
                .setParameter("eventType", eventType)
                .setParameter("payload", payload)
                .setParameter("traceparent", traceparent)
                .executeUpdate()
                .replaceWithVoid());
    }
}
