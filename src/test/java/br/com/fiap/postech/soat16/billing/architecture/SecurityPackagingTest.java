package br.com.fiap.postech.soat16.billing.architecture;

import static br.com.fiap.postech.soat16.billing.architecture.ArchitectureFixtures.PROJECT_ROOT;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * O Billing só valida tokens: nenhuma chave vai para o classpath, nenhuma chave privada é
 * versionada e a imagem não gera nem copia material de chave.
 */
@DisplayName("Empacotamento de material de chave")
class SecurityPackagingTest {

    private static final Path PACKAGED_RESOURCES = PROJECT_ROOT.resolve("src/main/resources");
    private static final Path THIS_TEST = PROJECT_ROOT.resolve(
            "src/test/java/br/com/fiap/postech/soat16/billing/architecture/SecurityPackagingTest.java");
    private static final List<Path> DOCKERFILES = List.of(
            PROJECT_ROOT.resolve("Dockerfile"),
            PROJECT_ROOT.resolve("infra/docker/Dockerfile"),
            PROJECT_ROOT.resolve("src/main/docker/Dockerfile"));
    private static final Set<String> IGNORED_DIRECTORIES = Set.of("target", ".git", ".local-jwt", ".scratch");

    private static final String PRIVATE_KEY_MARKER = "PRIVATE KEY-----";
    private static final String PUBLIC_KEY_MARKER = "PUBLIC KEY-----";

    @Test
    @DisplayName("nenhuma chave, privada ou pública, vai para o classpath da aplicação")
    void noKeyMaterialInPackagedResources() throws IOException {
        List<String> offenders = walk(PACKAGED_RESOURCES, path -> true,
                content -> content.contains(PRIVATE_KEY_MARKER) || content.contains(PUBLIC_KEY_MARKER));

        assertTrue(offenders.isEmpty(), () -> "Chave empacotada na imagem: " + offenders);
    }

    @Test
    @DisplayName("nenhuma chave privada é versionada em lugar algum do repositório")
    void noPrivateKeyAnywhereInRepository() throws IOException {
        List<String> offenders = walk(PROJECT_ROOT,
                path -> !path.equals(THIS_TEST) && !isUnderIgnoredDirectory(path),
                content -> content.contains(PRIVATE_KEY_MARKER));

        assertTrue(offenders.isEmpty(), () -> "Chave privada versionada: " + offenders);
    }

    @Test
    @DisplayName("o build da imagem não gera nem copia chave")
    void dockerfilesDoNotProduceKeyMaterial() throws IOException {
        for (Path dockerfile : DOCKERFILES) {
            if (!Files.exists(dockerfile)) {
                continue;
            }
            String content = Files.readString(dockerfile);
            assertFalse(content.contains("genpkey"), dockerfile + " gera chave no build");
            assertFalse(content.contains(".pem"), dockerfile + " copia chave para a imagem");
        }
    }

    private static boolean isUnderIgnoredDirectory(Path path) {
        Path relative = PROJECT_ROOT.relativize(path);
        for (Path segment : relative) {
            if (IGNORED_DIRECTORIES.contains(segment.toString())) {
                return true;
            }
        }
        return false;
    }

    private static List<String> walk(Path root, Predicate<Path> pathFilter, Predicate<String> contentFilter)
            throws IOException {
        if (!Files.exists(root)) {
            return List.of();
        }
        try (Stream<Path> files = Files.walk(root)) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(pathFilter)
                    .filter(path -> contentFilter.test(read(path)))
                    .map(path -> PROJECT_ROOT.relativize(path).toString())
                    .toList();
        }
    }

    // ISO_8859_1 mapeia qualquer byte para um caractere, então a busca funciona em binário.
    private static String read(Path file) {
        try {
            return new String(Files.readAllBytes(file), StandardCharsets.ISO_8859_1);
        } catch (IOException exception) {
            throw new IllegalStateException("Falha ao ler " + file, exception);
        }
    }
}
