# Regras arquiteturais executáveis

Toda regra marcada **[OBRIGATÓRIO]** nos documentos de arquitetura tem um teste que a cobra. Este
documento é a especificação desses testes: o que verificam, com que mecanismo e onde vivem. O agente
que criar o repositório implementa estas classes antes de qualquer código de produção; o agente que
adicionar uma regra atualiza este documento no mesmo PR.

Os testes ficam em `src/test/java/br/com/fiap/postech/soat16/billing/architecture/`, são `*Test`
(rodam no Surefire, sem container), usam JUnit 5 com `@Nested`/`@DisplayName` em português e, quando
ArchUnit, importam as classes **uma vez por JVM** a partir de um fixture compartilhado.

## Mecanismos

| Mecanismo | Para quê | Observação |
|---|---|---|
| **ArchUnit** (`com.tngtech.archunit:archunit-junit5`, 1.4.x) | Dependências entre pacotes, forma dos pacotes, nomenclatura, modificadores, anotações | Sobre bytecode; não enxerga anotações Lombok (retenção `SOURCE`) |
| **Varredura de código-fonte** (`Files.walk` em `src/main/java`) | Anotações Lombok proibidas no domínio | Mesmo mecanismo dos `*ArchitectureTest` do monólito, restrito ao que o bytecode não mostra |
| **Leitura de `application.yaml`** (SnakeYAML, já no classpath via `quarkus-config-yaml`) | Chaves que são decisão arquitetural | Não sobe o Quarkus |
| **Leitura de `pom.xml`** (DOM do JDK) | Gates de qualidade e escopo de dependências | Impede "ajuste para passar" |
| **Leitura do sistema de arquivos** | Material de chave, Dockerfile, nomes de migration | Herdado de `JwtKeyPackagingTest` do monólito |

### Fixture

```java
final class ArchitectureFixtures {
    static final String ROOT = "br.com.fiap.postech.soat16.billing";
    static final String SHARED = ROOT + ".shared..";
    static final String APP_EXCEPTION = ROOT + ".shared.domain.exception.AppException";
    static final Path PROJECT_ROOT = Path.of(System.getProperty("user.dir"));
    static final JavaClasses PRODUCTION_CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_JARS)
            .importPackages(ROOT);
    static final JavaClasses TEST_CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.ONLY_INCLUDE_TESTS)
            .importPackages(ROOT);
}
```

`src/test/resources/archunit.properties` com `archRule.failOnEmptyShould=false`: as regras existem
antes do código, e um pacote ainda vazio não pode reprovar o build. A partir da primeira classe no
pacote, a regra vale integralmente.

## Regras

Cada linha: regra → documento que a justifica → classe de teste → como.

### `HexagonalArchitectureTest` — [hexagonal-contract.md](hexagonal-contract.md)

| Regra | Como (ArchUnit) |
|---|---|
| Toda classe vive em `<ctx>.domain..`, `<ctx>.application..` ou `<ctx>.adapter..` | `classes().that().resideInAPackage(ROOT + "..").should().resideInAnyPackage(ROOT + ".*.domain..", ROOT + ".*.application..", ROOT + ".*.adapter..")` |
| Adaptadores declaram direção (`adapter.in..` ou `adapter.out..`) | `classes().that().resideInAPackage("..adapter..").should().resideInAnyPackage("..adapter.in..", "..adapter.out..")` |
| Domínio não depende de `application` nem `adapter` | `noClasses().that().resideInAPackage("..domain..").should().dependOnClassesThat().resideInAnyPackage("..application..", "..adapter..")` |
| Domínio livre de framework | `noClasses().that().resideInAPackage("..domain..").should().dependOnClassesThat().resideInAnyPackage("jakarta.persistence..", "jakarta.ws.rs..", "jakarta.enterprise..", "jakarta.inject..", "jakarta.validation..", "jakarta.annotation..", "org.hibernate..", "org.eclipse.microprofile..", "io.quarkus..", "io.smallrye..", "io.vertx..", "io.micrometer..", "io.opentelemetry..", "com.fasterxml.jackson..", "org.jboss..")` |
| Aplicação não depende de adaptadores | `noClasses().that().resideInAPackage("..application..").should().dependOnClassesThat().resideInAPackage("..adapter..")` |
| Aplicação não conhece transporte nem persistência (exceção: `@WithSession`/`@WithTransaction`) | `noClasses().that().resideInAPackage("..application..").should().dependOnClassesThat().resideInAnyPackage("jakarta.ws.rs..", "jakarta.persistence..", "org.hibernate..", "io.vertx..", "com.fasterxml.jackson..", "io.quarkus.hibernate.reactive.panache")` — o pacote raiz do Panache sem `..` exclui o subpacote `.common` |
| `adapter.in` e `adapter.out` não se conhecem | Duas regras `noClasses()...resideInAPackage("..adapter.in..")...dependOnClassesThat().resideInAPackage("..adapter.out..")` e a inversa |
| Portas de saída são interfaces | `classes().that().resideInAPackage("..application.port.out..").should().beInterfaces()` |
| Portas são implementadas só em `adapter.out` | `classes().that().implement(resideInAPackage("..application.port.out..")).should().resideInAPackage("..adapter.out..")` |
| `@Entity`/`@MappedSuperclass`/`@Embeddable` só em `adapter.out.persistence` | `classes().that().areAnnotatedWith("jakarta.persistence.Entity").or()...should().resideInAPackage("..adapter.out.persistence..")` |
| `@Path` só em `adapter.in.rest` | `classes().that().areAnnotatedWith("jakarta.ws.rs.Path").should().resideInAPackage("..adapter.in.rest..")` |
| `ExceptionMapper` só em `adapter.in.rest` | `classes().that().implement("jakarta.ws.rs.ext.ExceptionMapper").should().resideInAPackage("..adapter.in.rest..")` |
| Exceções de `domain`/`application` estendem `AppException` e vivem em `domain.exception` | `classes().that().resideInAnyPackage("..domain..", "..application..").and().areAssignableTo(Throwable.class).should().beAssignableTo(APP_EXCEPTION).andShould().resideInAPackage("..domain.exception..")` |
| Sem ciclos entre contextos | `slices().matching(ROOT + ".(*)..").should().beFreeOfCycles()` |
| Contexto só alcança outro por `adapter.out` → `application` do outro | `slices().matching(ROOT + ".(*)..").should().notDependOnEachOther().ignoreDependency(alwaysTrue(), resideInAPackage(SHARED)).ignoreDependency(resideInAPackage("..adapter.out.."), resideInAPackage("..application.."))` |
| `shared` não depende de contexto | `noClasses().that().resideInAPackage(SHARED).should().dependOnClassesThat(resideInAPackage(ROOT + "..").and(not(resideInAPackage(SHARED))))` |

### `NamingConventionsTest` — [hexagonal-contract.md](hexagonal-contract.md)

| Pacote / critério | Deve |
|---|---|
| `..adapter.in.rest.controller..` | `haveSimpleNameEndingWith("Controller")` |
| `..adapter.in.rest.openapi..` | `beInterfaces()`, `haveSimpleNameEndingWith("ControllerDocs")`, `beAnnotatedWith("jakarta.ws.rs.Path")` |
| `..adapter.in.rest.dto.request..` | `beRecords()`, sufixo `RequestDto` |
| `..adapter.in.rest.dto.response..` | `beRecords()`, sufixo `ResponseDto` |
| `..adapter.in.rest.mapper..` e `..adapter.out.persistence.mapper..` | sufixo `Mapper`, `haveModifier(FINAL)`, `haveOnlyPrivateConstructors()` |
| `implement("jakarta.ws.rs.ext.ExceptionMapper")` | sufixo `ExceptionMapper` |
| `ROOT + ".*.application"` com `@ApplicationScoped` | sufixo `Service` |
| `..application.command..` | `beRecords()`, sufixo `Command` ou `Query` |
| `..application.result..` | `beRecords()`, sufixo `Result` |
| `..application.port.out..` | sufixo `Port` |
| `@Entity` | sufixo `JpaEntity`, pacote `..adapter.out.persistence.entity..` |
| `..adapter.out..` que implementa porta | sufixo `Repository` ou `Adapter` |
| `..domain.exception..` assignável a `Throwable` | sufixo `Exception` |
| `..domain.exception..` enum | sufixo `ErrorCode` |

### `DomainModelConventionsTest` — [domain-decisions.md](domain-decisions.md)

| Regra | Como |
|---|---|
| Sem setter público no domínio | `noMethods().that().areDeclaredInClassesThat().resideInAPackage("..domain..").and().haveNameMatching("set[A-Z].*").should().bePublic()` — pega setters escritos à mão **e** gerados por Lombok, porque ambos existem no bytecode |
| Campos de instância privados | `fields().that().areDeclaredInClassesThat().resideInAPackage("..domain..").and().areNotStatic().should().bePrivate()` |
| Sem `@Setter`, `@Data`, `@AllArgsConstructor`, `@NoArgsConstructor`, `@Builder` em `src/main/java/**/domain/**` | Varredura de fonte: para cada `.java` cujo caminho contém `/domain/`, falhar se contiver a anotação como token isolado |

### `TestConventionsTest` — [../engineering/testing.md](../engineering/testing.md)

Sobre `TEST_CLASSES`:

| Regra | Como |
|---|---|
| Todo `@QuarkusTest` é `*IT` | `classes().that().areAnnotatedWith("io.quarkus.test.junit.QuarkusTest").should().haveSimpleNameEndingWith("IT")` |
| Todo `*IT` de primeiro nível é `@QuarkusTest` | `classes().that().haveSimpleNameEndingWith("IT").and().areTopLevelClasses().and().areNotInterfaces().should().beAnnotatedWith(...)` |
| Nenhum `*Test` sobe o Quarkus | `noClasses().that().haveSimpleNameEndingWith("Test").and().areTopLevelClasses().should().beAnnotatedWith(...)` |
| `*IT` não usa Mockito nem `@InjectMock` | `noClasses().that().haveSimpleNameEndingWith("IT").should().dependOnClassesThat(resideInAPackage("org.mockito..").or(resideInAPackage("io.quarkus.test.junit.mockito..")).or(name("io.quarkus.test.InjectMock")))` |

### `ConfigurationContractTest` — [../engineering/persistence.md](../engineering/persistence.md), [../engineering/security.md](../engineering/security.md)

Lê `src/main/resources/application.yaml` e afirma:

| Chave | Valor | Motivo |
|---|---|---|
| `quarkus.hibernate-orm.schema-management.strategy` | `validate` | ADR-0002 |
| `quarkus.flyway.migrate-at-start` | `true` | ADR-0002 |
| `quarkus.flyway.schemas` e `quarkus.hibernate-orm.database.default-schema` | `oficina_billing` | banco próprio |
| `quarkus.datasource.jdbc.url` / `reactive.url` | contêm `currentSchema=oficina_billing` / `search_path=oficina_billing` | banco próprio |
| `%test.quarkus.hibernate-orm.schema-management` e `%test.quarkus.datasource.devservices` | ausentes | o `*IT` migra com Flyway no container do `PostgresTestResource` |
| `quarkus.devservices.enabled` | `false` | sem banco implícito |
| `quarkus.security.jaxrs.deny-unannotated-endpoints` | `true` | nenhuma rota aberta por omissão |
| `mp.jwt.verify.publickey.location` | presente | valida com a chave pública do OS |
| `mp.jwt.verify.issuer` | `${JWT_ISSUER:oficina-api}` | mesmo issuer do OS |
| `smallrye.jwt.sign` e `smallrye.jwt.new-token` | ausentes | o Billing não emite token |
| `quarkus.management.enabled` / `port` | `true` / `9000` | health e métricas fora do gateway |
| `quarkus.log.console.json.enabled` | `true` | logs estruturados |

### `BuildContractTest` — [../engineering/quality-gates.md](../engineering/quality-gates.md)

Lê `pom.xml` e afirma:

| Regra | Como |
|---|---|
| Gate JaCoCo ≥ 0.80 em `LINE`, único `<limit>` | propriedade `jacoco.minimum-line-coverage` ≥ 0.80; plugin `jacoco-maven-plugin` com exatamente um `<limit>` cujo `<counter>` é `LINE` |
| Nenhuma exclusão de cobertura | plugin JaCoCo sem `<excludes>`; pom sem `sonar.coverage.exclusions` |
| `*IT` rodam por padrão | propriedade `skipITs` ausente ou diferente de `true`; `maven-failsafe-plugin` presente com goal `integration-test` |
| `quarkus-smallrye-jwt-build` só em `test` | toda dependência com esse `artifactId` (fora de `dependencyManagement`/`plugin`) tem `<scope>test</scope>` |
| Um único Hibernate | `quarkus-hibernate-reactive-panache` presente; `quarkus-hibernate-orm-panache` ausente |
| ArchUnit presente | `archunit-junit5` entre as dependências |

### `SecurityPackagingTest` — [../engineering/security.md](../engineering/security.md)

| Regra | Como |
|---|---|
| Nenhuma chave (privada **ou pública**) em `src/main/resources` | varrer arquivos procurando os marcadores PEM `PRIVATE KEY` / `PUBLIC KEY`; ler como ISO-8859-1 para aceitar binário |
| Nenhuma chave privada versionada em lugar algum | varrer o repositório, ignorando `target/`, `.git/`, `.local-jwt/`, `.scratch/` e o próprio teste |
| Dockerfile não gera nem copia chave | para cada `Dockerfile`, `infra/docker/Dockerfile`, `src/main/docker/Dockerfile` existente: sem `genpkey`, sem `.pem` |

### `MigrationConventionsTest` — [../engineering/persistence.md](../engineering/persistence.md)

| Regra | Como |
|---|---|
| Nome `V<n>__snake_case.sql` e versões únicas | regex `V(\d+)__[a-z0-9]+(_[a-z0-9]+)*\.sql` sobre `src/main/resources/db/migration` |
| Nenhuma migration referencia `oficina_mecanica` | busca case-insensitive no conteúdo |
| Nenhuma migration faz `CREATE SCHEMA` | o schema é do Flyway (`create-schemas: true`); a migration cria tabelas |

## Regras que ficam na revisão (não automatizáveis a custo razoável)

- Regra de negócio dentro do `Service` em vez do agregado (um `if` sobre estado do domínio no serviço).
- `*IT` cobrindo todos os status da tabela de [rest-contracts.md](rest-contracts.md) para a rota tocada.
- Mensagens de validação idênticas às do contrato.
- Abstração sem segundo uso, `Optional`/`Uni` aninhados, mapper com lógica.
- `@DisplayName` descrevendo comportamento, não implementação.

A skill `code-review` do time (quando instalada) e o checklist em
[../agents/workflow.md](../agents/workflow.md) cobrem estes pontos.
