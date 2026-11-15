package br.com.fiap.postech.soat16.billing.estimate.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.fiap.postech.soat16.billing.estimate.application.command.EstimateCreationFailureCommand;
import br.com.fiap.postech.soat16.billing.estimate.application.command.ProcessWorkOrderOpenedCommand;
import br.com.fiap.postech.soat16.billing.estimate.application.command.ProcessWorkOrderOpenedCommand.PartReferenceCommand;
import br.com.fiap.postech.soat16.billing.estimate.application.command.ProcessWorkOrderOpenedCommand.ServiceReferenceCommand;
import br.com.fiap.postech.soat16.billing.estimate.application.port.out.CatalogLookupPort;
import br.com.fiap.postech.soat16.billing.estimate.application.port.out.EstimatePersistencePort;
import br.com.fiap.postech.soat16.billing.estimate.application.port.out.EventInboxPort;
import br.com.fiap.postech.soat16.billing.estimate.application.port.out.EventOutboxPort;
import br.com.fiap.postech.soat16.billing.estimate.application.result.CatalogItemSnapshotResult;
import br.com.fiap.postech.soat16.billing.estimate.domain.model.Estimate;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
@DisplayName("EstimateCreationWorkflow — abertura da OS")
class EstimateCreationWorkflowTest {

    @Mock
    CatalogLookupPort catalog;
    @Mock
    EventInboxPort inbox;
    @Mock
    EstimatePersistencePort estimates;
    @Mock
    EventOutboxPort outbox;

    EstimateCreationService workflow;

    @BeforeEach
    void setUp() {
        workflow = new EstimateCreationService(catalog, inbox, estimates, outbox);
    }

    @Nested
    @DisplayName("mensagem recebida")
    class Process {

        @Test
        @DisplayName("cria orçamento com snapshot e evento de saída")
        void createsEstimateAndOutboxEvent() {
            var command = request(List.of(new ServiceReferenceCommand(SERVICE_ID)), List.of(new PartReferenceCommand(PART_ID, 2)));
            when(inbox.claim(EVENT_ID, WORK_ORDER_ID, "work-order.opened")).thenReturn(Uni.createFrom().item(true));
            when(catalog.findService(SERVICE_ID)).thenReturn(Uni.createFrom().item(Optional.of(
                    new CatalogItemSnapshotResult(SERVICE_ID, "Revisão", new BigDecimal("100.00"), true))));
            when(catalog.findPart(PART_ID)).thenReturn(Uni.createFrom().item(Optional.of(
                    new CatalogItemSnapshotResult(PART_ID, "Filtro", new BigDecimal("12.50"), true))));
            when(estimates.save(any())).thenReturn(Uni.createFrom().voidItem());
            when(outbox.recordCreated(any(), any())).thenReturn(Uni.createFrom().voidItem());

            assertEquals(WorkOrderOpenedOutcome.CREATED, workflow.process(command).await().indefinitely().outcome());

            ArgumentCaptor<Estimate> estimate = ArgumentCaptor.forClass(Estimate.class);
            verify(estimates).save(estimate.capture());
            assertEquals(new BigDecimal("125.00"), estimate.getValue().getTotalAmount());
            verify(outbox).recordCreated(estimate.getValue(), "00-11111111111111111111111111111111-2222222222222222-01");
        }

        @Test
        @DisplayName("publica falha sem persistir orçamento quando referência de serviço não existe")
        void recordsMissingServiceWithoutPartialEstimate() {
            var command = request(List.of(new ServiceReferenceCommand(SERVICE_ID)), List.of(new PartReferenceCommand(PART_ID, 1)));
            when(inbox.claim(EVENT_ID, WORK_ORDER_ID, "work-order.opened")).thenReturn(Uni.createFrom().item(true));
            when(catalog.findService(SERVICE_ID)).thenReturn(Uni.createFrom().item(Optional.empty()));
            when(outbox.recordFailed(any(), any())).thenReturn(Uni.createFrom().voidItem());

            assertEquals(WorkOrderOpenedOutcome.FAILED, workflow.process(command).await().indefinitely().outcome());

            ArgumentCaptor<EstimateCreationFailureCommand> failure =
                    ArgumentCaptor.forClass(EstimateCreationFailureCommand.class);
            verify(outbox).recordFailed(failure.capture(), any());
            assertEquals("SERVICE_ITEM_NOT_FOUND", failure.getValue().code());
            verify(estimates, never()).save(any());
            verify(catalog, never()).findPart(PART_ID);
        }

        @Test
        @DisplayName("marca serviço inativo como falho sem criar orçamento")
        void recordsInactiveService() {
            var command = request(List.of(new ServiceReferenceCommand(SERVICE_ID)), List.of());
            when(inbox.claim(EVENT_ID, WORK_ORDER_ID, "work-order.opened")).thenReturn(Uni.createFrom().item(true));
            when(catalog.findService(SERVICE_ID)).thenReturn(Uni.createFrom().item(Optional.of(
                    new CatalogItemSnapshotResult(SERVICE_ID, "Revisão", new BigDecimal("100.00"), false))));
            when(outbox.recordFailed(any(), any())).thenReturn(Uni.createFrom().voidItem());

            assertEquals(WorkOrderOpenedOutcome.FAILED, workflow.process(command).await().indefinitely().outcome());

            ArgumentCaptor<EstimateCreationFailureCommand> failure =
                    ArgumentCaptor.forClass(EstimateCreationFailureCommand.class);
            verify(outbox).recordFailed(failure.capture(), any());
            assertEquals("SERVICE_ITEM_INACTIVE", failure.getValue().code());
            verify(estimates, never()).save(any());
        }

        @Test
        @DisplayName("não cria orçamento para abertura sem solicitação inicial")
        void acceptsOpeningWithoutInitialRequest() {
            when(inbox.claim(EVENT_ID, WORK_ORDER_ID, "work-order.opened")).thenReturn(Uni.createFrom().item(true));

            assertEquals(WorkOrderOpenedOutcome.NO_INITIAL_REQUEST,
                    workflow.process(request(List.of(), List.of())).await().indefinitely().outcome());

            verify(catalog, never()).findService(any());
            verify(estimates, never()).save(any());
            verify(outbox, never()).recordCreated(any(), any());
            verify(outbox, never()).recordFailed(any(), any());
        }

        @Test
        @DisplayName("deduplica pelo eventId antes de consultar o catálogo")
        void ignoresDuplicateEvent() {
            when(inbox.claim(EVENT_ID, WORK_ORDER_ID, "work-order.opened")).thenReturn(Uni.createFrom().item(false));

            assertEquals(WorkOrderOpenedOutcome.DUPLICATE,
                    workflow.process(request(List.of(new ServiceReferenceCommand(SERVICE_ID)), List.of()))
                            .await().indefinitely().outcome());

            verify(catalog, never()).findService(any());
            verify(estimates, never()).save(any());
            verify(outbox, never()).recordCreated(any(), any());
        }
    }

    private static final UUID EVENT_ID = UUID.randomUUID();
    private static final UUID WORK_ORDER_ID = UUID.randomUUID();
    private static final UUID SERVICE_ID = UUID.randomUUID();
    private static final UUID PART_ID = UUID.randomUUID();

    private static ProcessWorkOrderOpenedCommand request(List<ServiceReferenceCommand> services, List<PartReferenceCommand> parts) {
        return new ProcessWorkOrderOpenedCommand(EVENT_ID, UUID.randomUUID(), UUID.randomUUID(), WORK_ORDER_ID,
                OffsetDateTime.now(), services, parts, "00-11111111111111111111111111111111-2222222222222222-01");
    }
}
