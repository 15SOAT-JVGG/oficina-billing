package br.com.fiap.postech.soat16.billing.estimate.application;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;

import br.com.fiap.postech.soat16.billing.estimate.application.command.EstimateCreationFailureCommand;
import br.com.fiap.postech.soat16.billing.estimate.application.command.ProcessWorkOrderOpenedCommand;
import br.com.fiap.postech.soat16.billing.estimate.application.port.out.CatalogLookupPort;
import br.com.fiap.postech.soat16.billing.estimate.application.port.out.EstimatePersistencePort;
import br.com.fiap.postech.soat16.billing.estimate.application.port.out.EventInboxPort;
import br.com.fiap.postech.soat16.billing.estimate.application.port.out.EventOutboxPort;
import br.com.fiap.postech.soat16.billing.estimate.application.result.CatalogItemSnapshotResult;
import br.com.fiap.postech.soat16.billing.estimate.application.result.WorkOrderOpenedResult;
import br.com.fiap.postech.soat16.billing.estimate.domain.model.Estimate;
import br.com.fiap.postech.soat16.billing.estimate.domain.model.EstimateItem;
import br.com.fiap.postech.soat16.billing.estimate.domain.model.EstimateItemType;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
public class EstimateCreationService {

    private static final String EVENT_TYPE = "work-order.opened";

    private final CatalogLookupPort catalog;
    private final EventInboxPort inbox;
    private final EstimatePersistencePort estimates;
    private final EventOutboxPort outbox;

    @WithTransaction
    public Uni<WorkOrderOpenedResult> process(ProcessWorkOrderOpenedCommand command) {
        return inbox.claim(command.eventId(), command.workOrderId(), EVENT_TYPE)
                .flatMap(claimed -> {
                    if (!claimed) {
                        return Uni.createFrom().item(new WorkOrderOpenedResult(WorkOrderOpenedOutcome.DUPLICATE));
                    }
                    if (!command.hasInitialRequest()) {
                        return Uni.createFrom().item(new WorkOrderOpenedResult(WorkOrderOpenedOutcome.NO_INITIAL_REQUEST));
                    }
                    return resolveItems(command).flatMap(resolution -> {
                        if (!resolution.failureCode().isEmpty()) {
                            return recordFailure(command, resolution.failureCode(), resolution.failureMessage());
                        }
                        Estimate estimate = Estimate.create(UUID.randomUUID(), command.workOrderId(),
                                command.eventId(), command.sagaId(), command.correlationId(), resolution.items(),
                                OffsetDateTime.now(ZoneOffset.UTC));
                        return estimates.save(estimate)
                                .chain(() -> outbox.recordCreated(estimate, command.traceparent()))
                                .replaceWith(new WorkOrderOpenedResult(WorkOrderOpenedOutcome.CREATED));
                    });
                });
    }

    private Uni<WorkOrderOpenedResult> recordFailure(ProcessWorkOrderOpenedCommand command,
            String code, String message) {
        var failure = new EstimateCreationFailureCommand(UUID.randomUUID(), command.workOrderId(), command.sagaId(),
                command.correlationId(), command.eventId(), code, message);
        return outbox.recordFailed(failure, command.traceparent())
                .replaceWith(new WorkOrderOpenedResult(WorkOrderOpenedOutcome.FAILED));
    }

    private Uni<Resolution> resolveItems(ProcessWorkOrderOpenedCommand command) {
        List<EstimateItem> items = new ArrayList<>();
        return resolveService(command, 0, items).flatMap(resolution -> resolution.failed()
                ? Uni.createFrom().item(resolution)
                : resolvePart(command, 0, items));
    }

    private Uni<Resolution> resolveService(ProcessWorkOrderOpenedCommand command, int index,
            List<EstimateItem> items) {
        if (index == command.services().size()) {
            return Uni.createFrom().item(Resolution.success(items));
        }
        UUID id = command.services().get(index).serviceItemId();
        return catalog.findService(id).flatMap(found -> {
            if (found.isEmpty()) {
                return Uni.createFrom().item(Resolution.failure("SERVICE_ITEM_NOT_FOUND", "Service item not found"));
            }
            CatalogItemSnapshotResult snapshot = found.orElseThrow();
            if (!snapshot.active()) {
                return Uni.createFrom().item(Resolution.failure("SERVICE_ITEM_INACTIVE", "Service item is inactive"));
            }
            items.add(new EstimateItem(snapshot.id(), EstimateItemType.SERVICE, snapshot.name(), 1,
                    snapshot.unitPrice()));
            return resolveService(command, index + 1, items);
        });
    }

    private Uni<Resolution> resolvePart(ProcessWorkOrderOpenedCommand command, int index,
            List<EstimateItem> items) {
        if (index == command.parts().size()) {
            return Uni.createFrom().item(Resolution.success(items));
        }
        var reference = command.parts().get(index);
        return catalog.findPart(reference.partId()).flatMap(found -> {
            if (found.isEmpty()) {
                return Uni.createFrom().item(Resolution.failure("PART_NOT_FOUND", "Part not found"));
            }
            CatalogItemSnapshotResult snapshot = found.orElseThrow();
            items.add(new EstimateItem(snapshot.id(), EstimateItemType.PART, snapshot.name(), reference.quantity(),
                    snapshot.unitPrice()));
            return resolvePart(command, index + 1, items);
        });
    }

    private record Resolution(List<EstimateItem> items, String failureCode, String failureMessage) {
        private static Resolution success(List<EstimateItem> items) {
            return new Resolution(List.copyOf(items), "", "");
        }

        private static Resolution failure(String code, String message) {
            return new Resolution(List.of(), code, message);
        }

        private boolean failed() {
            return !failureCode.isEmpty();
        }
    }
}
