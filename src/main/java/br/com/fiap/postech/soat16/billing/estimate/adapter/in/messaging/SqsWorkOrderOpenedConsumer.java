package br.com.fiap.postech.soat16.billing.estimate.adapter.in.messaging;

import java.net.URI;
import java.util.Optional;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import br.com.fiap.postech.soat16.billing.estimate.application.EstimateCreationService;
import br.com.fiap.postech.soat16.billing.shared.adapter.AwsCredentialsProviderFactory;

import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

@ApplicationScoped
@RequiredArgsConstructor
public class SqsWorkOrderOpenedConsumer {

    private final WorkOrderOpenedDecoder decoder;
    private final EstimateCreationService workflow;

    @ConfigProperty(name = "app.events.aws.sqs-queue-url")
    Optional<String> queueUrl;

    @ConfigProperty(name = "app.events.aws.endpoint")
    Optional<String> endpoint;

    @ConfigProperty(name = "app.events.aws.region", defaultValue = "us-east-1")
    String region;

    @ConfigProperty(name = "app.events.aws.access-key-id")
    Optional<String> accessKeyId;

    @ConfigProperty(name = "app.events.aws.secret-access-key")
    Optional<String> secretAccessKey;

    @ConfigProperty(name = "app.events.aws.session-token")
    Optional<String> sessionToken;

    volatile SqsAsyncClient client;

    @Scheduled(every = "${app.events.aws.poll-interval:2s}",
            concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    Uni<Void> scheduledPoll() {
        if (queueUrl.filter(value -> !value.isBlank()).isEmpty()) {
            return Uni.createFrom().voidItem();
        }
        return pollOnce().onFailure().invoke(error -> Log.error("Billing SQS consumer failed; message will retry", error))
                .onFailure().recoverWithNull().replaceWithVoid();
    }

    public Uni<Void> pollOnce() {
        if (queueUrl.filter(value -> !value.isBlank()).isEmpty()) {
            return Uni.createFrom().voidItem();
        }
        String queue = queueUrl.orElseThrow();
        var request = ReceiveMessageRequest.builder().queueUrl(queue).maxNumberOfMessages(1)
                .waitTimeSeconds(1).messageAttributeNames("All").build();
        var context = Vertx.currentContext();
        return Uni.createFrom().completionStage(() -> client().receiveMessage(request))
                .emitOn(task -> context.runOnContext(ignored -> task.run()))
                .flatMap(response -> {
                    if (response.messages().isEmpty()) {
                        return Uni.createFrom().voidItem();
                    }
                    var message = response.messages().getFirst();
                    var traceparent = Optional.ofNullable(message.messageAttributes().get("traceparent"))
                            .map(attribute -> attribute.stringValue()).orElse(null);
                    var command = decoder.decode(message.body(), traceparent);
                    return workflow.process(command)
                            .chain(() -> Uni.createFrom().completionStage(() -> client().deleteMessage(
                                    DeleteMessageRequest.builder().queueUrl(queue)
                                            .receiptHandle(message.receiptHandle()).build())))
                            .replaceWithVoid();
                });
    }

    private synchronized SqsAsyncClient client() {
        if (client == null) {
            var builder = SqsAsyncClient.builder().region(Region.of(region));
            AwsCredentialsProviderFactory.create(accessKeyId, secretAccessKey, sessionToken)
                    .ifPresent(builder::credentialsProvider);
            endpoint.filter(value -> !value.isBlank()).ifPresent(value -> builder.endpointOverride(URI.create(value)));
            client = builder.build();
        }
        return client;
    }

    @PreDestroy
    void close() {
        if (client != null) {
            client.close();
        }
    }
}
