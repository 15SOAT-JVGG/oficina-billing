package br.com.fiap.postech.soat16.billing.shared.test.infrastructure;

import java.util.Map;

import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.CreateTopicRequest;
import software.amazon.awssdk.services.sns.model.SubscribeRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.SetQueueAttributesRequest;

public class LocalStackEventResource implements QuarkusTestResourceLifecycleManager {

    private static final String REGION = "us-east-1";
    private static final String AWS_KEY = "test";
    private final LocalStackContainer localStack = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:3.8"))
            .withServices(LocalStackContainer.Service.SNS, LocalStackContainer.Service.SQS);

    @Override
    public Map<String, String> start() {
        localStack.start();
        var endpoint = localStack.getEndpointOverride(LocalStackContainer.Service.SQS);
        try (var sns = SnsClient.builder().endpointOverride(endpoint).region(Region.of(REGION))
                .credentialsProvider(credentials()).build();
                var sqs = SqsClient.builder().endpointOverride(endpoint).region(Region.of(REGION))
                        .credentialsProvider(credentials()).build()) {
            String osTopicArn = createTopic(sns, "os-events.fifo");
            String billingTopicArn = createTopic(sns, "billing-events.fifo");
            String billingQueueUrl = createQueue(sqs, "billing-events.fifo");
            String osQueueUrl = createQueue(sqs, "os-events.fifo");
            subscribe(sns, sqs, osTopicArn, billingQueueUrl, "[\"work-order.opened\"]");
            subscribe(sns, sqs, billingTopicArn, osQueueUrl,
                    "[\"estimate.created\",\"estimate.creation-failed\"]");
            return Map.ofEntries(
                    Map.entry("quarkus.scheduler.enabled", "false"),
                    Map.entry("app.events.aws.region", REGION),
                    Map.entry("app.events.aws.endpoint", endpoint.toString()),
                    Map.entry("app.events.aws.access-key-id", AWS_KEY),
                    Map.entry("app.events.aws.secret-access-key", AWS_KEY),
                    Map.entry("app.events.aws.sqs-queue-url", billingQueueUrl),
                    Map.entry("app.events.aws.sns-topic-arn", billingTopicArn),
                    Map.entry("app.events.aws.poll-interval", "24h"),
                    Map.entry("app.events.aws.outbox-interval", "24h"),
                    Map.entry("test.events.os-topic-arn", osTopicArn),
                    Map.entry("test.events.os-queue-url", osQueueUrl));
        }
    }

    private String createTopic(SnsClient sns, String name) {
        return sns.createTopic(CreateTopicRequest.builder().name(name)
                .attributes(Map.of("FifoTopic", "true", "ContentBasedDeduplication", "false")).build())
                .topicArn();
    }

    private String createQueue(SqsClient sqs, String name) {
        return sqs.createQueue(CreateQueueRequest.builder().queueName(name)
                .attributes(Map.of(QueueAttributeName.FIFO_QUEUE, "true"))
                .build()).queueUrl();
    }

    private void subscribe(SnsClient sns, SqsClient sqs, String topicArn, String queueUrl, String eventTypes) {
        String queueArn = sqs.getQueueAttributes(request -> request.queueUrl(queueUrl)
                .attributeNames(QueueAttributeName.QUEUE_ARN)).attributes().get(QueueAttributeName.QUEUE_ARN);
        String policy = "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\","
                + "\"Principal\":{\"Service\":\"sns.amazonaws.com\"},\"Action\":\"sqs:SendMessage\","
                + "\"Resource\":\"%s\",\"Condition\":{\"ArnEquals\":{\"aws:SourceArn\":\"%s\"}}}]}"
                        .formatted(queueArn, topicArn);
        sqs.setQueueAttributes(SetQueueAttributesRequest.builder().queueUrl(queueUrl)
                .attributes(Map.of(QueueAttributeName.POLICY, policy)).build());
        sns.subscribe(SubscribeRequest.builder().topicArn(topicArn).protocol("sqs").endpoint(queueArn)
                .attributes(Map.of("RawMessageDelivery", "true",
                        "FilterPolicy", "{\"type\":%s}".formatted(eventTypes)))
                .build());
    }

    private static StaticCredentialsProvider credentials() {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(AWS_KEY, AWS_KEY));
    }

    @Override
    public void stop() {
        localStack.stop();
    }
}
