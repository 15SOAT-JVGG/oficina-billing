package br.com.fiap.postech.soat16.billing.shared.adapter;

import java.util.Optional;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;

public final class AwsCredentialsProviderFactory {

    private AwsCredentialsProviderFactory() { }

    public static Optional<AwsCredentialsProvider> create(Optional<String> accessKeyId,
            Optional<String> secretAccessKey, Optional<String> sessionToken) {
        if (accessKeyId.filter(value -> !value.isBlank()).isEmpty()
                || secretAccessKey.filter(value -> !value.isBlank()).isEmpty()) {
            return Optional.empty();
        }
        String key = accessKeyId.orElseThrow();
        String secret = secretAccessKey.orElseThrow();
        AwsCredentials credentials = sessionToken.filter(value -> !value.isBlank())
                .<AwsCredentials>map(token -> AwsSessionCredentials.create(key, secret, token))
                .orElseGet(() -> AwsBasicCredentials.create(key, secret));
        return Optional.of(StaticCredentialsProvider.create(credentials));
    }
}
