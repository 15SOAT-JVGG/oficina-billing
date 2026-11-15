package br.com.fiap.postech.soat16.billing.estimate.adapter.out.messaging;

import java.net.URI;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

import jakarta.annotation.PreDestroy;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import br.com.fiap.postech.soat16.billing.shared.adapter.AwsCredentialsProviderFactory;

import io.smallrye.mutiny.Uni;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;

@Singleton
public class SnsOutboxTransport {

    @ConfigProperty(name = "app.events.aws.sns-topic-arn")
    Optional<String> topicArn;

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

    volatile SnsAsyncClient client;

    public boolean isConfigured() {
        return topicArn.filter(value -> !value.isBlank()).isPresent();
    }

    public Uni<Void> publish(UUID eventId, UUID workOrderId, String type, String payload, String traceparent) {
        var attributes = new HashMap<String, MessageAttributeValue>();
        attributes.put("type", attribute(type));
        if (traceparent != null && !traceparent.isBlank()) {
            attributes.put("traceparent", attribute(traceparent));
        }
        var request = PublishRequest.builder().topicArn(topicArn.orElseThrow())
                .message(payload).messageGroupId(workOrderId.toString())
                .messageDeduplicationId(eventId.toString()).messageAttributes(attributes).build();
        return Uni.createFrom().completionStage(() -> client().publish(request)).replaceWithVoid();
    }

    private static MessageAttributeValue attribute(String value) {
        return MessageAttributeValue.builder().dataType("String").stringValue(value).build();
    }

    private synchronized SnsAsyncClient client() {
        if (client == null) {
            var builder = SnsAsyncClient.builder().region(Region.of(region));
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
