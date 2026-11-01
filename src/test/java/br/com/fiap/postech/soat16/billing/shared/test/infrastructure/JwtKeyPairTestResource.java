package br.com.fiap.postech.soat16.billing.shared.test.infrastructure;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

/**
 * Gera um par RS256 efêmero e faz o teste representar o OS Service: a privada só serve para o
 * teste cunhar tokens; a aplicação recebe a pública e o issuer. Nenhuma chave é versionada.
 */
public class JwtKeyPairTestResource implements QuarkusTestResourceLifecycleManager {

    public static final String ISSUER = "oficina-api";

    private static final Path KEY_DIRECTORY = Path.of("target", "jwt-test");
    private static final int KEY_SIZE_BITS = 2048;
    private static final int PEM_LINE_LENGTH = 64;

    @Override
    public Map<String, String> start() {
        KeyPair keyPair = generateKeyPair();
        Path privateKeyLocation = writePem("privateKey.pem", "PRIVATE KEY", keyPair.getPrivate().getEncoded());
        Path publicKeyLocation = writePem("publicKey.pem", "PUBLIC KEY", keyPair.getPublic().getEncoded());

        return Map.of(
                "smallrye.jwt.sign.key.location", privateKeyLocation.toString(),
                "mp.jwt.verify.publickey.location", publicKeyLocation.toString(),
                "mp.jwt.verify.issuer", ISSUER);
    }

    @Override
    public void stop() {
        // O par vive em target/ e desaparece com o mvn clean.
    }

    private KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(KEY_SIZE_BITS);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("RSA indisponível na JVM de teste", exception);
        }
    }

    private Path writePem(String fileName, String label, byte[] encodedKey) {
        String base64 = Base64.getMimeEncoder(PEM_LINE_LENGTH, new byte[] {'\n'}).encodeToString(encodedKey);
        String pem = "-----BEGIN %s-----\n%s\n-----END %s-----\n".formatted(label, base64, label);

        try {
            Files.createDirectories(KEY_DIRECTORY);
            Path location = KEY_DIRECTORY.resolve(fileName);
            Files.writeString(location, pem, StandardCharsets.US_ASCII);
            return location;
        } catch (IOException exception) {
            throw new UncheckedIOException("Falha ao gravar o par RS256 de teste", exception);
        }
    }
}
