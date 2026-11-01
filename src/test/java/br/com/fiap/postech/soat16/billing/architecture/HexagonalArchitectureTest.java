package br.com.fiap.postech.soat16.billing.architecture;

import static br.com.fiap.postech.soat16.billing.architecture.ArchitectureFixtures.APP_EXCEPTION;
import static br.com.fiap.postech.soat16.billing.architecture.ArchitectureFixtures.PRODUCTION_CLASSES;
import static br.com.fiap.postech.soat16.billing.architecture.ArchitectureFixtures.ROOT;
import static br.com.fiap.postech.soat16.billing.architecture.ArchitectureFixtures.SHARED;
import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Contrato da arquitetura hexagonal")
class HexagonalArchitectureTest {

    private static final String DOMAIN = "..domain..";
    private static final String APPLICATION = "..application..";
    private static final String ADAPTER = "..adapter..";
    private static final String ADAPTER_IN = "..adapter.in..";
    private static final String ADAPTER_OUT = "..adapter.out..";
    private static final String PORT_OUT = "..application.port.out..";

    @Nested
    @DisplayName("Forma dos pacotes")
    class PackageShape {

        @Test
        @DisplayName("toda classe vive em <contexto>.domain, .application ou .adapter")
        void everyClassLivesInALayer() {
            classes().that().resideInAPackage(ROOT + "..")
                    .should().resideInAnyPackage(
                            ROOT + ".*.domain..", ROOT + ".*.application..", ROOT + ".*.adapter..")
                    .check(PRODUCTION_CLASSES);
        }

        @Test
        @DisplayName("adaptadores declaram direção: adapter.in ou adapter.out")
        void adaptersDeclareDirection() {
            classes().that().resideInAPackage(ADAPTER)
                    .should().resideInAnyPackage(ADAPTER_IN, ADAPTER_OUT)
                    .check(PRODUCTION_CLASSES);
        }
    }

    @Nested
    @DisplayName("Direção das dependências")
    class DependencyDirection {

        @Test
        @DisplayName("o domínio não depende de application nem de adapter")
        void domainDoesNotDependOnOuterLayers() {
            noClasses().that().resideInAPackage(DOMAIN)
                    .should().dependOnClassesThat().resideInAnyPackage(APPLICATION, ADAPTER)
                    .check(PRODUCTION_CLASSES);
        }

        @Test
        @DisplayName("o domínio é livre de framework")
        void domainIsFrameworkFree() {
            noClasses().that().resideInAPackage(DOMAIN)
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "jakarta.persistence..", "jakarta.ws.rs..", "jakarta.enterprise..",
                            "jakarta.inject..", "jakarta.validation..", "jakarta.annotation..",
                            "org.hibernate..", "org.eclipse.microprofile..", "io.quarkus..",
                            "io.smallrye..", "io.vertx..", "io.micrometer..", "io.opentelemetry..",
                            "com.fasterxml.jackson..", "org.jboss..")
                    .check(PRODUCTION_CLASSES);
        }

        @Test
        @DisplayName("a aplicação não depende de adaptadores")
        void applicationDoesNotDependOnAdapters() {
            noClasses().that().resideInAPackage(APPLICATION)
                    .should().dependOnClassesThat().resideInAPackage(ADAPTER)
                    .check(PRODUCTION_CLASSES);
        }

        // O pacote raiz do Panache sem ".." exclui o subpacote .common, onde vivem
        // @WithSession/@WithTransaction, a única infraestrutura permitida na aplicação.
        @Test
        @DisplayName("a aplicação não conhece transporte nem persistência")
        void applicationDoesNotKnowTransportOrPersistence() {
            noClasses().that().resideInAPackage(APPLICATION)
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "jakarta.ws.rs..", "jakarta.persistence..", "org.hibernate..", "io.vertx..",
                            "com.fasterxml.jackson..", "io.quarkus.hibernate.reactive.panache")
                    .check(PRODUCTION_CLASSES);
        }

        @Test
        @DisplayName("adapter.in não conhece adapter.out")
        void inboundAdaptersDoNotKnowOutbound() {
            noClasses().that().resideInAPackage(ADAPTER_IN)
                    .should().dependOnClassesThat().resideInAPackage(ADAPTER_OUT)
                    .check(PRODUCTION_CLASSES);
        }

        @Test
        @DisplayName("adapter.out não conhece adapter.in")
        void outboundAdaptersDoNotKnowInbound() {
            noClasses().that().resideInAPackage(ADAPTER_OUT)
                    .should().dependOnClassesThat().resideInAPackage(ADAPTER_IN)
                    .check(PRODUCTION_CLASSES);
        }
    }

    @Nested
    @DisplayName("Portas e adaptadores")
    class PortsAndAdapters {

        @Test
        @DisplayName("portas de saída são interfaces")
        void outboundPortsAreInterfaces() {
            classes().that().resideInAPackage(PORT_OUT)
                    .should().beInterfaces()
                    .check(PRODUCTION_CLASSES);
        }

        @Test
        @DisplayName("portas são implementadas só em adapter.out")
        void portsAreImplementedOnlyInOutboundAdapters() {
            classes().that().implement(resideInAPackage(PORT_OUT))
                    .should().resideInAPackage(ADAPTER_OUT)
                    .check(PRODUCTION_CLASSES);
        }

        @Test
        @DisplayName("@Entity, @MappedSuperclass e @Embeddable só em adapter.out.persistence")
        void jpaAnnotationsOnlyInPersistenceAdapter() {
            classes().that().areAnnotatedWith("jakarta.persistence.Entity")
                    .or().areAnnotatedWith("jakarta.persistence.MappedSuperclass")
                    .or().areAnnotatedWith("jakarta.persistence.Embeddable")
                    .should().resideInAPackage("..adapter.out.persistence..")
                    .check(PRODUCTION_CLASSES);
        }

        @Test
        @DisplayName("@Path só em adapter.in.rest")
        void pathOnlyInRestAdapter() {
            classes().that().areAnnotatedWith("jakarta.ws.rs.Path")
                    .should().resideInAPackage("..adapter.in.rest..")
                    .check(PRODUCTION_CLASSES);
        }

        @Test
        @DisplayName("ExceptionMapper só em adapter.in.rest")
        void exceptionMappersOnlyInRestAdapter() {
            classes().that().implement("jakarta.ws.rs.ext.ExceptionMapper")
                    .should().resideInAPackage("..adapter.in.rest..")
                    .check(PRODUCTION_CLASSES);
        }

        @Test
        @DisplayName("exceções de domain e application estendem AppException e vivem em domain.exception")
        void exceptionsExtendAppException() {
            classes().that().resideInAnyPackage(DOMAIN, APPLICATION)
                    .and().areAssignableTo(Throwable.class)
                    .should().beAssignableTo(APP_EXCEPTION)
                    .andShould().resideInAPackage("..domain.exception..")
                    .check(PRODUCTION_CLASSES);
        }
    }

    @Nested
    @DisplayName("Entre contextos")
    class BetweenContexts {

        @Test
        @DisplayName("não há ciclos entre contextos")
        void noCyclesBetweenContexts() {
            slices().matching(ROOT + ".(*)..")
                    .should().beFreeOfCycles()
                    .check(PRODUCTION_CLASSES);
        }

        @Test
        @DisplayName("um contexto só alcança outro por adapter.out → application do outro")
        void contextsOnlyReachEachOtherThroughOutboundAdapters() {
            slices().matching(ROOT + ".(*)..")
                    .should().notDependOnEachOther()
                    .ignoreDependency(alwaysTrue(), resideInAPackage(SHARED))
                    .ignoreDependency(resideInAPackage(ADAPTER_OUT), resideInAPackage(APPLICATION))
                    .check(PRODUCTION_CLASSES);
        }

        @Test
        @DisplayName("shared não depende de nenhum contexto")
        void sharedDoesNotDependOnContexts() {
            noClasses().that().resideInAPackage(SHARED)
                    .should().dependOnClassesThat(
                            resideInAPackage(ROOT + "..").and(not(resideInAPackage(SHARED))))
                    .check(PRODUCTION_CLASSES);
        }
    }
}
