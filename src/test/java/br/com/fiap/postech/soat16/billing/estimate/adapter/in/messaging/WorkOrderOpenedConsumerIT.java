package br.com.fiap.postech.soat16.billing.estimate.adapter.in.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import br.com.fiap.postech.soat16.billing.catalog.application.port.out.PartPersistencePort;
import br.com.fiap.postech.soat16.billing.catalog.application.port.out.ServiceCatalogPersistencePort;
import br.com.fiap.postech.soat16.billing.catalog.domain.model.Part;
import br.com.fiap.postech.soat16.billing.catalog.domain.model.PartType;
import br.com.fiap.postech.soat16.billing.catalog.domain.model.ServiceItem;
import br.com.fiap.postech.soat16.billing.estimate.adapter.out.messaging.OutboxPublisher;
import br.com.fiap.postech.soat16.billing.shared.test.infrastructure.LocalStackEventResource;
import br.com.fiap.postech.soat16.billing.shared.test.infrastructure.PostgresTestResource;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.vertx.VertxContextSupport;
import io.smallrye.mutiny.Uni;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
@QuarkusTestResource(LocalStackEventResource.class)
@DisplayName("SqsWorkOrderOpenedConsumer — fluxo Billing com broker e Postgres")
class WorkOrderOpenedConsumerIT {

    private static final String TRACEPARENT = "00-11111111111111111111111111111111-2222222222222222-01";

    @Inject
    SqsWorkOrderOpenedConsumer consumer;

    @Inject
    OutboxPublisher publisher;

    @Inject
    ServiceCatalogPersistencePort serviceCatalog;

    @Inject
    PartPersistencePort parts;

    @Inject
    ObjectMapper mapper;

    @ConfigProperty(name = "app.events.aws.endpoint")
    String endpoint;

    @ConfigProperty(name = "test.events.os-topic-arn")
    String osTopicArn;

    @ConfigProperty(name = "test.events.os-queue-url")
    String osQueueUrl;

    @ConfigProperty(name = "app.events.aws.sqs-queue-url")
    String billingQueueUrl;

    @Test
    @DisplayName("cria o snapshot no Postgres e publica estimate.created via SNS/SQS FIFO")
    void createsEstimateAndPublishesCreatedEvent() throws Exception {
        UUID serviceId = createService();
        UUID partId = createPart();
        UUID workOrderId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        String payload = openedEvent(eventId, workOrderId, serviceId, partId, 2);

        publishToOsTopic(payload, eventId, workOrderId);
        await(() -> consumer.pollOnce());

        assertEquals(1L, count("estimate", "work_order_id", workOrderId));
        assertEquals(2L, count("estimate_item", "estimate_id", estimateId(workOrderId)));
        assertEquals(0, new BigDecimal("125.00").compareTo(total(workOrderId)));
        assertTrue(await(() -> publisher.publishNext()));

        Message message = receiveOutput();
        JsonNode event = mapper.readTree(message.body());
        assertEquals("estimate.created", event.path("type").asText());
        assertEquals(eventId.toString(), event.path("causationId").asText());
        assertEquals("Revisão inicial", event.path("data").path("services").get(0).path("name").asText());
        assertEquals("Filtro", event.path("data").path("parts").get(0).path("name").asText());
        assertEquals(TRACEPARENT, message.messageAttributes().get("traceparent").stringValue());
    }

    @Test
    @DisplayName("referência inválida publica falha e não persiste orçamento parcial")
    void invalidReferenceDoesNotPersistPartialEstimate() throws Exception {
        UUID serviceId = createService();
        UUID missingPartId = UUID.randomUUID();
        UUID workOrderId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        publishToOsTopic(openedEvent(eventId, workOrderId, serviceId, missingPartId, 1), eventId, workOrderId);
        await(() -> consumer.pollOnce());

        assertEquals(0L, count("estimate", "work_order_id", workOrderId));
        assertEquals(1L, count("billing_event_inbox", "event_id", eventId));
        assertTrue(await(() -> publisher.publishNext()));

        JsonNode event = mapper.readTree(receiveOutput().body());
        assertEquals("estimate.creation-failed", event.path("type").asText());
        assertEquals(eventId.toString(), event.path("causationId").asText());
        assertEquals("PART_NOT_FOUND", event.path("data").path("code").asText());
        assertFalse(await(() -> publisher.publishNext()));
    }

    @Test
    @DisplayName("mensagem repetida mantém um orçamento e um único evento de saída")
    void duplicateDeliveryHasNoRepeatedEffects() throws Exception {
        UUID serviceId = createService();
        UUID workOrderId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        String payload = openedEvent(eventId, workOrderId, serviceId, null, 0);

        sendToBillingQueue(payload, workOrderId, UUID.randomUUID());
        sendToBillingQueue(payload, workOrderId, UUID.randomUUID());
        await(() -> consumer.pollOnce());
        await(() -> consumer.pollOnce());

        assertEquals(1L, count("estimate", "work_order_id", workOrderId));
        assertEquals(1L, count("billing_event_inbox", "event_id", eventId));
        assertEquals(1L, countOutbox(workOrderId));
        assertTrue(await(() -> publisher.publishNext()));
        JsonNode event = mapper.readTree(receiveOutput().body());
        assertEquals("estimate.created", event.path("type").asText());
        assertFalse(await(() -> publisher.publishNext()));
    }

    @Test
    @DisplayName("abertura sem itens é deduplicada sem criar orçamento ou publicar resultado")
    void openingWithoutInitialItemsProducesNoEstimate() throws Exception {
        UUID workOrderId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        publishToOsTopic(openedEvent(eventId, workOrderId, null, null, 0), eventId, workOrderId);
        await(() -> consumer.pollOnce());

        assertEquals(1L, count("billing_event_inbox", "event_id", eventId));
        assertEquals(0L, count("estimate", "work_order_id", workOrderId));
        assertEquals(0L, countOutbox(workOrderId));
        assertFalse(await(() -> publisher.publishNext()));
    }

    private UUID createService() {
        ServiceItem service = ServiceItem.register("Revisão inicial", null, new BigDecimal("100.00"), 60, true);
        return inTransaction(() -> serviceCatalog.save(service)).getId();
    }

    private UUID createPart() {
        Part part = Part.register("Filtro", null, new BigDecimal("12.50"), "UN", PartType.PART);
        return inTransaction(() -> parts.save(part)).getId();
    }

    private void publishToOsTopic(String payload, UUID eventId, UUID workOrderId) {
        try (var sns = SnsClient.builder().endpointOverride(java.net.URI.create(endpoint)).region(Region.US_EAST_1)
                .credentialsProvider(credentials()).build()) {
            sns.publish(PublishRequest.builder().topicArn(osTopicArn).message(payload)
                    .messageGroupId(workOrderId.toString()).messageDeduplicationId(eventId.toString())
                    .messageAttributes(Map.of(
                            "type", attribute("work-order.opened"),
                            "traceparent", attribute(TRACEPARENT)))
                    .build());
        }
    }

    private void sendToBillingQueue(String payload, UUID workOrderId, UUID deduplicationId) {
        try (var sqs = SqsClient.builder().endpointOverride(java.net.URI.create(endpoint)).region(Region.US_EAST_1)
                .credentialsProvider(credentials()).build()) {
            sqs.sendMessage(SendMessageRequest.builder().queueUrl(billingQueueUrl).messageBody(payload)
                    .messageGroupId(workOrderId.toString()).messageDeduplicationId(deduplicationId.toString()).build());
        }
    }

    private Message receiveOutput() {
        try (var sqs = SqsClient.builder().endpointOverride(java.net.URI.create(endpoint)).region(Region.US_EAST_1)
                .credentialsProvider(credentials()).build()) {
            var response = sqs.receiveMessage(ReceiveMessageRequest.builder().queueUrl(osQueueUrl)
                    .maxNumberOfMessages(1).waitTimeSeconds(2).messageAttributeNames("All").build());
            assertFalse(response.messages().isEmpty(), "esperava um evento de Billing na fila do OS");
            Message message = response.messages().getFirst();
            sqs.deleteMessage(DeleteMessageRequest.builder().queueUrl(osQueueUrl)
                    .receiptHandle(message.receiptHandle()).build());
            return message;
        }
    }

    private String openedEvent(UUID eventId, UUID workOrderId, UUID serviceId, UUID partId, int quantity)
            throws Exception {
        ObjectNode root = mapper.createObjectNode();
        root.put("eventId", eventId.toString());
        root.put("type", "work-order.opened");
        root.put("version", 1);
        root.put("sagaId", UUID.randomUUID().toString());
        root.put("correlationId", UUID.randomUUID().toString());
        root.putNull("causationId");
        root.put("workOrderId", workOrderId.toString());
        root.put("occurredAt", Instant.now().toString());
        ObjectNode data = root.putObject("data");
        var services = data.putArray("services");
        if (serviceId != null) {
            services.addObject().put("serviceItemId", serviceId.toString());
        }
        var parts = data.putArray("parts");
        if (partId != null) {
            parts.addObject().put("partId", partId.toString()).put("quantity", quantity);
        }
        return mapper.writeValueAsString(root);
    }

    private long count(String table, String column, UUID value) {
        return inTransaction(() -> Panache.getSession().flatMap(session -> session.createNativeQuery(
                "SELECT count(*) FROM oficina_billing." + table + " WHERE " + column + " = :value", Long.class)
                .setParameter("value", value).getSingleResult()));
    }

    private long countOutbox(UUID workOrderId) {
        return inTransaction(() -> Panache.getSession().flatMap(session -> session.createNativeQuery("""
                SELECT count(*) FROM oficina_billing.billing_event_outbox WHERE work_order_id = :workOrderId
                """, Long.class).setParameter("workOrderId", workOrderId).getSingleResult()));
    }

    private UUID estimateId(UUID workOrderId) {
        return inTransaction(() -> Panache.getSession().flatMap(session -> session.createNativeQuery("""
                SELECT estimate_id FROM oficina_billing.estimate WHERE work_order_id = :workOrderId
                """, UUID.class).setParameter("workOrderId", workOrderId).getSingleResult()));
    }

    private BigDecimal total(UUID workOrderId) {
        return inTransaction(() -> Panache.getSession().flatMap(session -> session.createNativeQuery("""
                SELECT total_amount FROM oficina_billing.estimate WHERE work_order_id = :workOrderId
                """, BigDecimal.class).setParameter("workOrderId", workOrderId).getSingleResult()));
    }

    private <T> T await(Supplier<Uni<T>> action) {
        try {
            return VertxContextSupport.subscribeAndAwait(action::get);
        } catch (Throwable exception) {
            throw new IllegalStateException("Falha ao executar operação assíncrona do teste", exception);
        }
    }

    private <T> T inTransaction(Supplier<Uni<T>> action) {
        try {
            return VertxContextSupport.subscribeAndAwait(() -> Panache.withTransaction(action));
        } catch (Throwable exception) {
            throw new IllegalStateException("Falha ao preparar ou consultar o banco de teste", exception);
        }
    }

    private static StaticCredentialsProvider credentials() {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test"));
    }

    private static MessageAttributeValue attribute(String value) {
        return MessageAttributeValue.builder().dataType("String").stringValue(value).build();
    }
}
