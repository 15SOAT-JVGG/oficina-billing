package br.com.fiap.postech.soat16.billing.architecture;

import static br.com.fiap.postech.soat16.billing.architecture.ArchitectureFixtures.TEST_CLASSES;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Convenções de teste: *Test unitário, *IT com Quarkus")
class TestConventionsTest {

    private static final String QUARKUS_TEST = "io.quarkus.test.junit.QuarkusTest";

    @Test
    @DisplayName("todo @QuarkusTest termina em IT")
    void everyQuarkusTestIsAnIntegrationTest() {
        classes().that().areAnnotatedWith(QUARKUS_TEST)
                .should().haveSimpleNameEndingWith("IT")
                .check(TEST_CLASSES);
    }

    @Test
    @DisplayName("todo *IT de primeiro nível é @QuarkusTest")
    void everyIntegrationTestBootsQuarkus() {
        classes().that().haveSimpleNameEndingWith("IT")
                .and().areTopLevelClasses()
                .and().areNotInterfaces()
                .should().beAnnotatedWith(QUARKUS_TEST)
                .check(TEST_CLASSES);
    }

    @Test
    @DisplayName("nenhum *Test sobe o Quarkus")
    void unitTestsDoNotBootQuarkus() {
        noClasses().that().haveSimpleNameEndingWith("Test")
                .and().areTopLevelClasses()
                .should().beAnnotatedWith(QUARKUS_TEST)
                .check(TEST_CLASSES);
    }

    @Test
    @DisplayName("*IT não usa Mockito nem @InjectMock")
    void integrationTestsDoNotMock() {
        noClasses().that().haveSimpleNameEndingWith("IT")
                .should().dependOnClassesThat(
                        resideInAPackage("org.mockito..")
                                .or(resideInAPackage("io.quarkus.test.junit.mockito.."))
                                .or(name("io.quarkus.test.InjectMock")))
                .check(TEST_CLASSES);
    }
}
