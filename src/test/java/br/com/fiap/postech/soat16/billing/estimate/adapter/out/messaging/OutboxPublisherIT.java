package br.com.fiap.postech.soat16.billing.estimate.adapter.out.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.function.Supplier;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

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
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
@QuarkusTestResource(LocalStackEventResource.class)
@DisplayName("OutboxPublisher — retentativa de publicação com SNS FIFO")
class OutboxPublisherIT {

    @Inject
    OutboxPublisher publisher;

    @Inject
    SnsOutboxTransport transport;

    @Inject
    ObjectMapper mapper;

    @ConfigProperty(name = "app.events.aws.endpoint")
    String endpoint;

    @ConfigProperty(name = "test.events.os-queue-url")
    String osQueueUrl;

    @Test
    void keepsEventPendingAfterBrokerFailureAndPublishesItOnRetry() throws Exception {
        var event = mapper.readTree(Files.readString(Path.of("docs/events/v1/estimate-created.example.json")));
        UUID eventId = UUID.fromString(event.path("eventId").asText());
        UUID workOrderId = UUID.fromString(event.path("workOrderId").asText());
        insertOutbox(eventId, workOrderId, mapper.writeValueAsString(event));

        SnsAsyncClient availableClient = transport.client;
        var unavailableClient = SnsAsyncClient.builder().region(Region.US_EAST_1)
                .endpointOverride(URI.create("http://127.0.0.1:1"))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
                .build();
        transport.client = unavailableClient;
        assertFalse(await(() -> publisher.publishNext().onFailure().recoverWithItem(false)));
        assertEquals(1L, pendingOutboxCount(eventId));

        unavailableClient.close();
        transport.client = availableClient;
        assertTrue(await(() -> publisher.publishNext()));
        assertEquals(0L, pendingOutboxCount(eventId));
        assertFalse(receiveEventBody().isBlank());
        assertFalse(await(() -> publisher.publishNext()));
        assertFalse(hasQueuedOutput());
    }

    private void insertOutbox(UUID eventId, UUID workOrderId, String payload) {
        await(() -> Panache.withTransaction(() -> Panache.getSession().flatMap(session -> session.createNativeQuery("""
                INSERT INTO oficina_billing.billing_event_outbox
                    (event_id, work_order_id, event_type, payload, created_at)
                VALUES (:eventId, :workOrderId, 'estimate.created', CAST(:payload AS jsonb), now())
                """).setParameter("eventId", eventId).setParameter("workOrderId", workOrderId)
                .setParameter("payload", payload).executeUpdate().replaceWithVoid())));
    }

    private long pendingOutboxCount(UUID eventId) {
        return await(() -> Panache.withTransaction(() -> Panache.getSession().flatMap(session -> session
                .createNativeQuery("""
                        SELECT count(*) FROM oficina_billing.billing_event_outbox
                        WHERE event_id = :eventId AND published_at IS NULL
                        """, Long.class).setParameter("eventId", eventId).getSingleResult())));
    }

    private String receiveEventBody() {
        try (var sqs = SqsClient.builder().endpointOverride(java.net.URI.create(endpoint)).region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
                .build()) {
            var response = sqs.receiveMessage(ReceiveMessageRequest.builder().queueUrl(osQueueUrl)
                    .maxNumberOfMessages(1).waitTimeSeconds(2).build());
            assertFalse(response.messages().isEmpty(), "esperava o evento pendente após a retentativa");
            sqs.deleteMessage(request -> request.queueUrl(osQueueUrl)
                    .receiptHandle(response.messages().getFirst().receiptHandle()));
            return response.messages().getFirst().body();
        }
    }

    private boolean hasQueuedOutput() {
        try (var sqs = SqsClient.builder().endpointOverride(URI.create(endpoint)).region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
                .build()) {
            return !sqs.receiveMessage(ReceiveMessageRequest.builder().queueUrl(osQueueUrl)
                    .maxNumberOfMessages(1).waitTimeSeconds(1).build()).messages().isEmpty();
        }
    }

    private <T> T await(Supplier<Uni<T>> action) {
        try {
            return VertxContextSupport.subscribeAndAwait(action::get);
        } catch (Throwable exception) {
            throw new IllegalStateException("Falha ao executar operação assíncrona do teste", exception);
        }
    }
}
