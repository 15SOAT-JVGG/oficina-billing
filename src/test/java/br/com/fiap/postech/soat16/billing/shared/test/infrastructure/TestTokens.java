package br.com.fiap.postech.soat16.billing.shared.test.infrastructure;

import java.time.Duration;
import java.util.Set;

import io.smallrye.jwt.build.Jwt;

/**
 * Tokens cunhados pelo teste no papel do OS Service, com o issuer e a claim groups que ele emite.
 */
public final class TestTokens {

    private TestTokens() {
    }

    public static String tokenFor(String... roles) {
        return Jwt.issuer(JwtKeyPairTestResource.ISSUER)
                .upn("integration-test")
                .groups(Set.of(roles))
                .expiresIn(Duration.ofHours(1))
                .sign();
    }

    public static String bearer(String... roles) {
        return "Bearer " + tokenFor(roles);
    }

    public static String tokenFromOtherIssuer(String... roles) {
        return Jwt.issuer("another-issuer")
                .upn("integration-test")
                .groups(Set.of(roles))
                .expiresIn(Duration.ofHours(1))
                .sign();
    }
}
