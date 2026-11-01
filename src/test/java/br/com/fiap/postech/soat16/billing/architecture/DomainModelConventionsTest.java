package br.com.fiap.postech.soat16.billing.architecture;

import static br.com.fiap.postech.soat16.billing.architecture.ArchitectureFixtures.PRODUCTION_CLASSES;
import static br.com.fiap.postech.soat16.billing.architecture.ArchitectureFixtures.PROJECT_ROOT;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Convenções do modelo de domínio")
class DomainModelConventionsTest {

    private static final Path MAIN_SOURCES = PROJECT_ROOT.resolve("src/main/java");
    private static final List<String> FORBIDDEN_LOMBOK = List.of(
            "@Setter", "@Data", "@AllArgsConstructor", "@NoArgsConstructor", "@Builder");

    // Pega setters escritos à mão e gerados por Lombok: ambos existem no bytecode.
    @Test
    @DisplayName("não há setter público no domínio")
    void noPublicSettersInDomain() {
        noMethods().that().areDeclaredInClassesThat().resideInAPackage("..domain..")
                .and().haveNameMatching("set[A-Z].*")
                .should().bePublic()
                .check(PRODUCTION_CLASSES);
    }

    @Test
    @DisplayName("campos de instância do domínio são privados")
    void domainInstanceFieldsArePrivate() {
        fields().that().areDeclaredInClassesThat().resideInAPackage("..domain..")
                .and().areNotStatic()
                .should().bePrivate()
                .check(PRODUCTION_CLASSES);
    }

    // Anotações Lombok têm retenção SOURCE, então só a varredura do código-fonte as enxerga.
    @Test
    @DisplayName("o domínio não usa Lombok que gere estado mutável, construtor público ou builder")
    void domainDoesNotUseForbiddenLombok() throws IOException {
        List<String> offenders;
        try (Stream<Path> sources = Files.walk(MAIN_SOURCES)) {
            offenders = sources
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> path.toString().contains("/domain/"))
                    .filter(DomainModelConventionsTest::usesForbiddenLombok)
                    .map(path -> PROJECT_ROOT.relativize(path).toString())
                    .toList();
        }

        assertTrue(offenders.isEmpty(), () -> "Lombok proibido no domínio: " + offenders);
    }

    private static boolean usesForbiddenLombok(Path source) {
        try {
            String content = Files.readString(source);
            return FORBIDDEN_LOMBOK.stream()
                    .anyMatch(annotation -> Pattern.compile("(?m)^\\s*" + Pattern.quote(annotation) + "\\b")
                            .matcher(content).find());
        } catch (IOException exception) {
            throw new IllegalStateException("Falha ao ler " + source, exception);
        }
    }
}
