---
name: write-postgres-it
description: Escrever um teste de integração *IT do oficina-billing com @QuarkusTest, Postgres real via Testcontainers, token JWT RS256 cunhado pelo teste e asserções sobre sucesso e erros (400, 401, 403, 404, 409). Use para *ControllerIT, *RepositoryIT e *OpenApiContractIT.
---

# Escrever um `*IT` com Postgres real

Por que existe: o `oficina-app` não tem `*IT` HTTP para o catálogo e os seus `*IT` nunca rodaram no
CI; cada detalhe abaixo (recursos, contexto Vert.x, token, corpo de erro) foi um ponto de tropeço.
O que é regra está em `docs/engineering/testing.md`; aqui está o passo a passo.

## Pré-requisitos

- Docker em execução (`docker info`). Sem Docker, pare: registre a limitação no PR e deixe o CI rodar.
- `PostgresTestResource` e `JwtKeyPairTestResource` existentes em
  `src/test/java/br/com/fiap/postech/soat16/billing/shared/test/infrastructure/`. Se não existirem,
  porte-os dos homônimos do `oficina-app` (`shared/test/infrastructure/`) trocando o schema para
  `oficina_billing`; `JwtKeyPairTestResource` deve injetar também `mp.jwt.verify.issuer=oficina-api`.
- A migration da tabela já existe: o Flyway migra o container na subida do Quarkus.

## Cabeçalho obrigatório

```java
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
@QuarkusTestResource(JwtKeyPairTestResource.class)
@DisplayName("<Agregado>Controller — <o que cobre>")
class <Agregado>ControllerIT {
```

Nome termina em `IT` (Failsafe). Sem `@InjectMock`, sem Mockito (`TestConventionsTest` reprova).

## Token

```java
private static String tokenFor(String... roles) {
    return Jwt.issuer("oficina-api").upn("integration-test").groups(Set.of(roles))
            .expiresIn(Duration.ofHours(1)).sign();
}
```

Para autenticar todas as requisições da classe, use o filtro do `WorkOrderControllerIT` do monólito
(`RestAssured.filters(...)` em `@BeforeAll`, `RestAssured.replaceFiltersWith(List.of())` em
`@AfterAll`) — mas então os testes de `401` e `403` precisam **sobrescrever** o header (`401`: filtro
desligado ou header vazio; `403`: `tokenFor("MECHANIC")`). Prefira passar o token por requisição
quando a classe mistura papéis.

## Dados

Preferência: criar pela própria API (`POST` → extrair `id`). Quando precisar do repositório:

```java
@Inject <Agregado>Repository repository;

private <T> T inTransaction(Supplier<Uni<T>> action) {
    try {
        return VertxContextSupport.subscribeAndAwait(() -> Panache.withTransaction(action));
    } catch (Throwable exception) {
        throw new IllegalStateException("Falha ao preparar dados de teste", exception);
    }
}
```

Nomes únicos (`"Troca de oleo - " + UUID.randomUUID()`); nunca limpar tabelas.

## Matriz mínima por operação

Copie a linha da rota em `docs/architecture/rest-contracts.md` e escreva um `@Nested` por operação
com um `@Test` por célula:

| Cenário | Dado | Asserção |
|---|---|---|
| Sucesso | token ADMIN, corpo válido | status; campos relevantes; `Location` no `201` (`header("Location", endsWith("/admin/<recurso>/" + id))`) |
| `400` por campo | cada validação do `*RequestDto` | `statusCode(400)`, `body("error", equalTo("Validation Error"))`, `body("violations", hasItem("<campo>: <mensagem exata>"))` |
| `401` | sem header | `statusCode(401)` |
| `403` | `tokenFor("MECHANIC")` em escrita | `statusCode(403)` |
| `404` | `UUID.randomUUID()` | `statusCode(404)`, `body("type", equalTo("NOT_FOUND"))`, `body("code", equalTo("<AGREGADO>_NOT_FOUND"))` |
| `409`/`422` | só se a rota os tiver na tabela | `type` e `code` correspondentes |

Leitura (`GET`) também prova `200` com `tokenFor("MECHANIC")`.

## `*RepositoryIT`

Mesmo cabeçalho. Cobre: `save` novo devolve `id`; `findXById` de id inexistente emite `null`;
`save` de existente atualiza (`copyState`); `deleteXById` devolve `true`/`false`; constraints da
migration (`NOT NULL`, tamanho) reprovam com exceção de persistência. Tudo via `inTransaction(...)`.

## `*OpenApiContractIT`

Modelo: `WorkOrderOpenApiContractIT` do monólito. Busca `/q/openapi?format=json` uma vez, e para
cada operação da tabela do contrato afirma que `responses` contém exatamente os status da tabela
(inclusive `401` e `403`).

## Executar

```shell
./mvnw --batch-mode verify -Dit.test=<Agregado>ControllerIT -Dsurefire.skip=true   # só este IT
./mvnw --batch-mode verify                                                           # tudo, antes do PR
```

Falha ao subir o Quarkus com "Schema-validation: missing table/column" significa migration e entidade
divergentes: corrija a migration (nunca a estratégia de schema).
