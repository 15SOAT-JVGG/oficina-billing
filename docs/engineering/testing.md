# Testes

Dois níveis, dois sufixos, duas fases do Maven. A cobertura mínima é de **80% de linhas no bundle**,
medida sobre a soma dos dois níveis, e é gate do `verify`. Um bom teste verifica comportamento pela
interface pública (resposta HTTP, estado lido pela API, exceção lançada pelo agregado), não a
implementação.

## Convenções

| Sufixo | O que é | Infraestrutura | Fase Maven | Quem roda |
|---|---|---|---|---|
| `*Test` | Unitário ou de arquitetura; dependências mockadas com Mockito | Nenhuma (sem Quarkus, sem Docker) | `test` (Surefire) | `./mvnw test` e `./mvnw verify` |
| `*IT` | Integração: `@QuarkusTest` + Postgres real via Testcontainers | Docker | `integration-test` (Failsafe) | `./mvnw verify` |

**[OBRIGATÓRIO]** Essas regras são verificadas por `TestConventionsTest`
([executable-rules.md](../architecture/executable-rules.md)): `@QuarkusTest` só em `*IT`, `*IT`
sempre `@QuarkusTest`, `*Test` nunca sobe o Quarkus, `*IT` não usa Mockito.

**[OBRIGATÓRIO]** O pacote do teste espelha o da classe testada. Cenários agrupados com `@Nested`,
descritos com `@DisplayName` em português, nomeando o comportamento esperado
("deve recusar preço zero"), não o método ("testRegister").

**[OBRIGATÓRIO]** Nenhum teste depende da ordem de execução ou de estado deixado por outro. Dados de
teste usam identificadores únicos (`UUID.randomUUID()` no nome) em vez de limpar tabelas.

## O que testar em cada camada

| Camada | Teste | O que cobre |
|---|---|---|
| `domain.model` | `*Test` puro, sem mock | Cada factory, cada método de comportamento, **cada invariante** (caminho válido e cada violação com a exceção esperada), `equals`/`hashCode` por identidade |
| `application` | `*Test` com a porta mockada (`@Mock` + `MockitoExtension`) | Orquestração: carrega, delega, persiste; "não encontrado" vira a exceção certa; nada de regra de negócio reimplementada no teste |
| `adapter.in.rest.mapper` e `adapter.out.persistence.mapper` | `*Test` puro | Ida e volta de cada campo; `null` tratado |
| `adapter.in.rest.controller` | `*Test` com o `Service` mockado | Status e `Location` do `201`, `204` sem corpo, propagação de falha do `Uni` (modelo: `ServiceItemControllerTest` do monólito) |
| Rota completa | `*IT` HTTP com REST-assured | Ver abaixo |
| `adapter.out.persistence` | `*RepositoryIT` | Mapeamento JPA contra o schema **migrado pelo Flyway**, constraints, `null` em "não encontrado", `false` em delete inexistente |
| Contrato OpenAPI | `*OpenApiContractIT` | Status documentados por operação batem com [rest-contracts.md](../architecture/rest-contracts.md) |
| Arquitetura | `architecture/*Test` | [executable-rules.md](../architecture/executable-rules.md) |

**[OBRIGATÓRIO]** Para cada classe nova ou alterada: caminho de sucesso, cada ramo condicional,
cada invariante e cada exceção. Cobertura abaixo de 80% é sintoma; a correção é testar o que falta,
nunca excluir da medição ([quality-gates.md](quality-gates.md)).

## `*IT` de rota

**[OBRIGATÓRIO]** Todo controller tem um `*ControllerIT` que, para **cada operação**, cobre:

- o status de sucesso e o corpo (campos relevantes do `*ResponseDto`, `Location` no `201`);
- `400` com corpo do `ValidationExceptionMapper` para cada validação do `*RequestDto`;
- `401` sem token;
- `403` com token de papel insuficiente (MECHANIC em escrita);
- `404` para id inexistente, afirmando `status`, `type`, `code` do `ApiErrorResponseDto`;
- `409`/`422` quando a rota os tiver (ver [rest-contracts.md](../architecture/rest-contracts.md)).

Esqueleto (fonte: `WorkOrderControllerIT` e `SecurityIT` do monólito, adaptados ao Billing):

```java
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
@QuarkusTestResource(JwtKeyPairTestResource.class)
@DisplayName("ServiceItemController — rotas do catálogo de serviços")
class ServiceItemControllerIT {

    private static final String SERVICES_PATH = "/admin/services";

    private static String tokenFor(String... roles) {
        return Jwt.issuer("oficina-api").upn("integration-test").groups(Set.of(roles))
                .expiresIn(Duration.ofHours(1)).sign();
    }

    @Nested
    @DisplayName("POST /admin/services")
    class Create {

        @Test
        @DisplayName("deve criar o serviço e devolver 201 com Location")
        void shouldCreate() {
            given().header("Authorization", "Bearer " + tokenFor("ADMIN"))
                .contentType(ContentType.JSON)
                .body("""
                    {"name":"Troca de oleo","basePrice":80.00,"estimatedDurationMinutes":30,"active":true}
                    """)
            .when().post(SERVICES_PATH)
            .then().statusCode(201)
                .header("Location", containsString(SERVICES_PATH + "/"))
                .body("id", notNullValue())
                .body("name", equalTo("Troca de oleo"));
        }

        @Test
        @DisplayName("deve recusar o MECHANIC com 403")
        void shouldForbidMechanic() { /* token MECHANIC → 403 */ }

        @Test
        @DisplayName("deve devolver 400 com as violações quando faltar o preço")
        void shouldValidate() {
            // ... .statusCode(400).body("error", equalTo("Validation Error"))
            //        .body("violations", hasItem("basePrice: Base price is required"));
        }
    }
}
```

**[OBRIGATÓRIO]** Os `*IT` recebem banco e chaves por `QuarkusTestResource`:

- `PostgresTestResource` sobe `postgres:16-alpine`, cria o schema `oficina_billing` e injeta
  `quarkus.datasource.jdbc.url`, `quarkus.datasource.reactive.url`, usuário e senha. O Flyway migra
  o container na inicialização, com as mesmas migrations do ambiente real.
- `JwtKeyPairTestResource` gera um par RS256 em `target/jwt-test/`, injeta
  `smallrye.jwt.sign.key.location` (só para o teste cunhar tokens) e
  `mp.jwt.verify.publickey.location` + `mp.jwt.verify.issuer=oficina-api` (o que a aplicação valida).
  É o teste fazendo o papel do OS Service.

Ambos vivem em `src/test/java/.../billing/shared/test/infrastructure/` e são portados dos homônimos
do monólito (`shared/test/infrastructure/`), trocando o schema para `oficina_billing`.

**[OBRIGATÓRIO]** Semear dados em `*IT` fora do event loop, estabelecendo o contexto Vert.x que o
Hibernate Reactive exige:

```java
private <T> T inTransaction(Supplier<Uni<T>> action) {
    try {
        return VertxContextSupport.subscribeAndAwait(() -> Panache.withTransaction(action));
    } catch (Throwable exception) {
        throw new IllegalStateException("Falha ao preparar dados de teste", exception);
    }
}
```

**[RECOMENDADO]** Preferir semear pela própria API (`POST` e guardar o `id`) a injetar o repositório:
o teste fica no nível do contrato e não conhece o mapeamento.

**[EVITAR]** `Thread.sleep`, `@Order`, `@TestMethodOrder`, limpar tabelas em `@AfterEach`,
`@InjectMock` em `*IT`, asserções em implementação interna (consultar o banco para conferir o que a
API já devolve).

## Testes unitários de domínio

**[OBRIGATÓRIO]** O teste do agregado é a especificação dos invariantes. Para cada linha da tabela de
invariantes em [domain-decisions.md](../architecture/domain-decisions.md): um teste que passa com o
valor válido e um que afirma a exceção (`assertThrows(InvalidServiceItemException.class, ...)`) e o
`ErrorCode`.

**[EVITAR]** Teste tautológico: afirmar `getName()` depois de passar `name` ao construtor não testa
nada. Teste de *getter* existe só se há transformação (normalização, default).

**[EVITAR]** Reflexão para injetar `id` (o monólito faz `Field.setAccessible(true)` em
`ServiceItemTest`); use o factory `restore(...)`.

## Mocks

**[OBRIGATÓRIO]** Mockar só na fronteira: a porta (`*Port`) no teste do `Service`; o `Service` no
teste do controller. Nunca mockar agregado, value object, mapper ou record.

**[OBRIGATÓRIO]** `@ExtendWith(MockitoExtension.class)` + `@Mock`; `when(...).thenReturn(Uni.createFrom().item(...))`;
`verify` só para efeito colateral que o retorno não prova (chamada ao `save` no update).

## Evidência

**[OBRIGATÓRIO]** O PR cita o resultado do `./mvnw verify` (contagem de testes por fase e percentual
do JaCoCo em `target/jacoco-report/index.html`). O formato está em
[../agents/workflow.md](../agents/workflow.md).
