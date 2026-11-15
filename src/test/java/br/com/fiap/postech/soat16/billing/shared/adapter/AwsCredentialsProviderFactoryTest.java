package br.com.fiap.postech.soat16.billing.shared.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;

@DisplayName("AwsCredentialsProviderFactory — configuração das credenciais AWS")
class AwsCredentialsProviderFactoryTest {

    @Test
    void omitsProviderWhenCredentialsAreNotConfigured() {
        assertTrue(AwsCredentialsProviderFactory.create(Optional.empty(), Optional.empty(), Optional.empty())
                .isEmpty());
    }

    @Test
    void createsBasicCredentialsWhenNoSessionTokenIsPresent() {
        var provider = AwsCredentialsProviderFactory.create(Optional.of("key"), Optional.of("secret"),
                Optional.empty()).orElseThrow();

        var credentials = assertInstanceOf(AwsBasicCredentials.class, provider.resolveCredentials());
        assertEquals("key", credentials.accessKeyId());
        assertEquals("secret", credentials.secretAccessKey());
    }

    @Test
    void createsSessionCredentialsWhenSessionTokenIsPresent() {
        var provider = AwsCredentialsProviderFactory.create(Optional.of("key"), Optional.of("secret"),
                Optional.of("token")).orElseThrow();

        var credentials = assertInstanceOf(AwsSessionCredentials.class, provider.resolveCredentials());
        assertEquals("token", credentials.sessionToken());
    }
}
