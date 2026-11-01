package br.com.fiap.postech.soat16.billing.architecture;

import static br.com.fiap.postech.soat16.billing.architecture.ArchitectureFixtures.PROJECT_ROOT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Lê o pom.xml e impede que o build seja "ajustado para passar": gate de cobertura, *IT por
 * padrão, escopo das dependências sensíveis e o único Hibernate.
 */
@DisplayName("Contrato do pom.xml")
class BuildContractTest {

    private static final Path POM = PROJECT_ROOT.resolve("pom.xml");
    private static final BigDecimal MINIMUM_COVERAGE = new BigDecimal("0.80");

    private static Document pom;

    @BeforeAll
    static void loadPom() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        pom = factory.newDocumentBuilder().parse(POM.toFile());
        pom.getDocumentElement().normalize();
    }

    private static List<Element> elements(Element parent, String tagName) {
        List<Element> result = new ArrayList<>();
        NodeList children = parent.getElementsByTagName(tagName);
        for (int index = 0; index < children.getLength(); index++) {
            result.add((Element) children.item(index));
        }
        return result;
    }

    private static List<Element> directChildren(Element parent, String tagName) {
        List<Element> result = new ArrayList<>();
        NodeList children = parent.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            Node child = children.item(index);
            if (child instanceof Element element && tagName.equals(element.getTagName())) {
                result.add(element);
            }
        }
        return result;
    }

    private static String childText(Element parent, String tagName) {
        List<Element> found = directChildren(parent, tagName);
        return found.isEmpty() ? null : found.get(0).getTextContent().trim();
    }

    private static String property(String name) {
        Element properties = elements(pom.getDocumentElement(), "properties").get(0);
        return childText(properties, name);
    }

    private static Element plugin(String artifactId) {
        return elements(pom.getDocumentElement(), "plugin").stream()
                .filter(candidate -> artifactId.equals(childText(candidate, "artifactId")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Plugin ausente: " + artifactId));
    }

    private static List<Element> declaredDependencies(String artifactId) {
        return elements(pom.getDocumentElement(), "dependency").stream()
                .filter(candidate -> artifactId.equals(childText(candidate, "artifactId")))
                .filter(candidate -> !hasAncestor(candidate, "dependencyManagement"))
                .filter(candidate -> !hasAncestor(candidate, "plugin"))
                .toList();
    }

    private static boolean hasAncestor(Node node, String tagName) {
        Node current = node.getParentNode();
        while (current != null) {
            if (tagName.equals(current.getNodeName())) {
                return true;
            }
            current = current.getParentNode();
        }
        return false;
    }

    @Nested
    @DisplayName("Cobertura (JaCoCo)")
    class Coverage {

        @Test
        @DisplayName("o gate é de pelo menos 80% de linhas")
        void minimumLineCoverage() {
            String minimum = property("jacoco.minimum-line-coverage");
            assertNotNull(minimum, "propriedade jacoco.minimum-line-coverage ausente");
            assertTrue(new BigDecimal(minimum).compareTo(MINIMUM_COVERAGE) >= 0,
                    "gate de cobertura abaixo de 80%: " + minimum);
        }

        @Test
        @DisplayName("há exatamente um limite e ele conta linhas")
        void singleLineLimit() {
            Element jacoco = plugin("jacoco-maven-plugin");
            List<Element> limits = elements(jacoco, "limit");
            assertEquals(1, limits.size(), "esperado um único <limit>");
            assertEquals("LINE", childText(limits.get(0), "counter"));
        }

        @Test
        @DisplayName("nenhuma classe é excluída da medição")
        void noCoverageExclusions() {
            Element jacoco = plugin("jacoco-maven-plugin");
            assertTrue(elements(jacoco, "excludes").isEmpty(), "JaCoCo com <excludes>");
            assertEquals(0, pom.getElementsByTagName("sonar.coverage.exclusions").getLength(),
                    "pom com sonar.coverage.exclusions");
        }
    }

    @Nested
    @DisplayName("Testes de integração")
    class IntegrationTests {

        @Test
        @DisplayName("os *IT rodam por padrão")
        void integrationTestsRunByDefault() {
            assertNotEquals("true", property("skipITs"), "skipITs=true desliga os *IT");
            Element failsafe = plugin("maven-failsafe-plugin");
            boolean runsIntegrationTests = elements(failsafe, "goal").stream()
                    .anyMatch(goal -> "integration-test".equals(goal.getTextContent().trim()));
            assertTrue(runsIntegrationTests, "Failsafe sem o goal integration-test");
        }
    }

    @Nested
    @DisplayName("Dependências")
    class Dependencies {

        @Test
        @DisplayName("quarkus-smallrye-jwt-build só em escopo de teste")
        void jwtBuildOnlyInTests() {
            List<Element> declarations = declaredDependencies("quarkus-smallrye-jwt-build");
            assertFalse(declarations.isEmpty(), "quarkus-smallrye-jwt-build ausente");
            declarations.forEach(declaration ->
                    assertEquals("test", childText(declaration, "scope"),
                            "quarkus-smallrye-jwt-build fora do escopo test"));
        }

        @Test
        @DisplayName("um único Hibernate: o reativo")
        void singleHibernate() {
            assertFalse(declaredDependencies("quarkus-hibernate-reactive-panache").isEmpty());
            assertTrue(declaredDependencies("quarkus-hibernate-orm-panache").isEmpty(),
                    "quarkus-hibernate-orm-panache não entra (ADR-0001)");
        }

        @Test
        @DisplayName("ArchUnit está presente")
        void archUnitPresent() {
            assertFalse(declaredDependencies("archunit-junit5").isEmpty());
        }
    }
}
