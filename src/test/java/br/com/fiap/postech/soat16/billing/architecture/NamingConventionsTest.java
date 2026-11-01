package br.com.fiap.postech.soat16.billing.architecture;

import static br.com.fiap.postech.soat16.billing.architecture.ArchitectureFixtures.PRODUCTION_CLASSES;
import static br.com.fiap.postech.soat16.billing.architecture.ArchitectureFixtures.ROOT;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaModifier.FINAL;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Convenções de nomenclatura por camada")
class NamingConventionsTest {

    private static final String JAX_RS_PATH = "jakarta.ws.rs.Path";
    private static final String JPA_ENTITY = "jakarta.persistence.Entity";

    @Nested
    @DisplayName("Adaptador REST")
    class RestAdapter {

        @Test
        @DisplayName("controllers terminam em Controller")
        void controllers() {
            classes().that().resideInAPackage("..adapter.in.rest.controller..")
                    .should().haveSimpleNameEndingWith("Controller")
                    .check(PRODUCTION_CLASSES);
        }

        @Test
        @DisplayName("interfaces de documentação terminam em ControllerDocs e carregam @Path")
        void controllerDocs() {
            classes().that().resideInAPackage("..adapter.in.rest.openapi..")
                    .should().beInterfaces()
                    .andShould().haveSimpleNameEndingWith("ControllerDocs")
                    .andShould().beAnnotatedWith(JAX_RS_PATH)
                    .check(PRODUCTION_CLASSES);
        }

        @Test
        @DisplayName("DTOs de requisição são records terminados em RequestDto")
        void requestDtos() {
            classes().that().resideInAPackage("..adapter.in.rest.dto.request..")
                    .should().beRecords()
                    .andShould().haveSimpleNameEndingWith("RequestDto")
                    .check(PRODUCTION_CLASSES);
        }

        @Test
        @DisplayName("DTOs de resposta são records terminados em ResponseDto")
        void responseDtos() {
            classes().that().resideInAPackage("..adapter.in.rest.dto.response..")
                    .should().beRecords()
                    .andShould().haveSimpleNameEndingWith("ResponseDto")
                    .check(PRODUCTION_CLASSES);
        }

        @Test
        @DisplayName("ExceptionMappers terminam em ExceptionMapper")
        void exceptionMappers() {
            classes().that().implement("jakarta.ws.rs.ext.ExceptionMapper")
                    .should().haveSimpleNameEndingWith("ExceptionMapper")
                    .check(PRODUCTION_CLASSES);
        }
    }

    @Nested
    @DisplayName("Mappers")
    class Mappers {

        @Test
        @DisplayName("mappers REST e de persistência são finais, sem instância e terminam em Mapper")
        void mappersAreStaticUtilities() {
            classes().that().resideInAnyPackage(
                            "..adapter.in.rest.mapper..", "..adapter.out.persistence.mapper..")
                    .should().haveSimpleNameEndingWith("Mapper")
                    .andShould().haveModifier(FINAL)
                    .andShould().haveOnlyPrivateConstructors()
                    .check(PRODUCTION_CLASSES);
        }
    }

    @Nested
    @DisplayName("Aplicação")
    class Application {

        @Test
        @DisplayName("beans da aplicação terminam em Service")
        void services() {
            classes().that().resideInAPackage(ROOT + ".*.application")
                    .and().areAnnotatedWith("jakarta.enterprise.context.ApplicationScoped")
                    .should().haveSimpleNameEndingWith("Service")
                    .check(PRODUCTION_CLASSES);
        }

        @Test
        @DisplayName("comandos e consultas são records terminados em Command ou Query")
        void commandsAndQueries() {
            classes().that().resideInAPackage("..application.command..")
                    .should().beRecords()
                    .andShould().haveSimpleNameEndingWith("Command")
                    .orShould().haveSimpleNameEndingWith("Query")
                    .check(PRODUCTION_CLASSES);
        }

        @Test
        @DisplayName("resultados são records terminados em Result")
        void results() {
            classes().that().resideInAPackage("..application.result..")
                    .should().beRecords()
                    .andShould().haveSimpleNameEndingWith("Result")
                    .check(PRODUCTION_CLASSES);
        }

        @Test
        @DisplayName("portas de saída terminam em Port")
        void ports() {
            classes().that().resideInAPackage("..application.port.out..")
                    .should().haveSimpleNameEndingWith("Port")
                    .check(PRODUCTION_CLASSES);
        }
    }

    @Nested
    @DisplayName("Persistência")
    class Persistence {

        @Test
        @DisplayName("entidades JPA terminam em JpaEntity e vivem em adapter.out.persistence.entity")
        void jpaEntities() {
            classes().that().areAnnotatedWith(JPA_ENTITY)
                    .should().haveSimpleNameEndingWith("JpaEntity")
                    .andShould().resideInAPackage("..adapter.out.persistence.entity..")
                    .check(PRODUCTION_CLASSES);
        }

        @Test
        @DisplayName("implementações de porta terminam em Repository ou Adapter")
        void portImplementations() {
            classes().that().resideInAPackage("..adapter.out..")
                    .and().implement(resideInAPackage("..application.port.out.."))
                    .should().haveSimpleNameEndingWith("Repository")
                    .orShould().haveSimpleNameEndingWith("Adapter")
                    .check(PRODUCTION_CLASSES);
        }
    }

    @Nested
    @DisplayName("Domínio")
    class Domain {

        @Test
        @DisplayName("exceções de domínio terminam em Exception")
        void exceptions() {
            classes().that().resideInAPackage("..domain.exception..")
                    .and().areAssignableTo(Throwable.class)
                    .should().haveSimpleNameEndingWith("Exception")
                    .check(PRODUCTION_CLASSES);
        }

        // ErrorType também é enum em shared.domain.exception, por isso o critério é implementar
        // ErrorCode, e não apenas ser enum.
        @Test
        @DisplayName("enums de código de erro terminam em ErrorCode")
        void errorCodes() {
            classes().that().resideInAPackage("..domain.exception..")
                    .and().areEnums()
                    .and().implement(ROOT + ".shared.domain.exception.ErrorCode")
                    .should().haveSimpleNameEndingWith("ErrorCode")
                    .check(PRODUCTION_CLASSES);
        }
    }
}
