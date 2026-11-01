package br.com.fiap.postech.soat16.billing.architecture;

import static br.com.fiap.postech.soat16.billing.architecture.ArchitectureFixtures.PROJECT_ROOT;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

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
                path -> !path.equals(THIS_TEST),
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

    // Diretórios ignorados são podados antes de entrar neles: target/ muda durante o próprio build
    // (o Surefire grava relatórios enquanto o teste roda) e um Files.walk plano falharia.
    private static List<String> walk(Path root, Predicate<Path> pathFilter, Predicate<String> contentFilter)
            throws IOException {
        List<String> offenders = new ArrayList<>();
        if (!Files.exists(root)) {
            return offenders;
        }
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes) {
                return IGNORED_DIRECTORIES.contains(directory.getFileName().toString())
                        ? FileVisitResult.SKIP_SUBTREE
                        : FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
                if (attributes.isRegularFile() && pathFilter.test(file) && contentFilter.test(read(file))) {
                    offenders.add(PROJECT_ROOT.relativize(file).toString());
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return offenders;
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
