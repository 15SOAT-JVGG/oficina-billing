# oficina-billing

Serviço de **Billing** da oficina mecânica (FIAP 16SOAT — Tech Challenge, Fase 4,
[spec #106](https://github.com/15SOAT-JVGG/oficina-app/issues/106)). Hoje detém o **catálogo de
serviços e peças com preço**, extraído do monólito [`oficina-app`](https://github.com/15SOAT-JVGG/oficina-app)
([issue #107](https://github.com/15SOAT-JVGG/oficina-app/issues/107)); orçamento, decisão do
cliente e cobrança (Mercado Pago) chegam nas issues seguintes. Saldo de estoque não é deste
serviço (é do Execution).

## Stack

Java 21 · Quarkus 3.32 · Hibernate Reactive + Panache · PostgreSQL 16 (banco e schema
`oficina_billing` próprios) · Flyway · SmallRye JWT (RS256, só validação) · OpenAPI/Swagger ·
JUnit 5, Mockito, REST-assured, Testcontainers, ArchUnit · JaCoCo ≥ 80% · SonarCloud.

## Como subir

```shell
docker compose up                      # Postgres + aplicação, sem configuração prévia
# API: http://localhost:8080  ·  Swagger: http://localhost:8080/q/swagger-ui  ·  Health: http://localhost:9000/q/health
```

Para desenvolvimento:

```shell
docker compose up -d postgres
cp ../oficina-app/.local-jwt/publicKey.pem .local-jwt/publicKey.pem   # chave pública do OS Service
POSTGRES_PORT=5433 ./mvnw quarkus:dev
```

O Billing **não emite token**: valida os JWT de ADMIN/MECHANIC emitidos pelo OS Service com a
chave pública dele (`JWT_PUBLIC_KEY_LOCATION`) e o mesmo issuer (`JWT_ISSUER`, padrão
`oficina-api`). Variáveis de ambiente em [`.env.example`](.env.example).

## Rotas

Mesmos caminhos, verbos, status, papéis, DTOs e mensagens de validação do `oficina-app`
([contrato](docs/architecture/rest-contracts.md)):

| Recurso | Rotas | Leitura | Escrita |
|---|---|---|---|
| Serviços | `GET/POST /admin/services`, `GET/PUT/DELETE /admin/services/{id}` | ADMIN, MECHANIC | ADMIN |
| Peças | `GET/POST /admin/parts`, `GET/PUT/DELETE /admin/parts/{id}` | ADMIN, MECHANIC | ADMIN |

As rotas de estoque do monólito (`/admin/parts/low-stock`, `PATCH /admin/parts/{id}/stock`) não
existem aqui.

## Arquitetura

Hexagonal por bounded context (`catalog`, `shared`), domínio rico, um único modelo de erro
(`AppException` → `ApiErrorResponseDto`), Flyway dono do schema com Hibernate em `validate`.
Regras verificadas por ArchUnit e testes de contrato. Documentação em [`AGENTS.md`](AGENTS.md),
[`CONTEXT.md`](CONTEXT.md) e [`docs/`](docs/).

## Build e qualidade

```shell
./mvnw test                 # unitários e testes de arquitetura (*Test)
./mvnw verify               # gate completo: + *IT com Postgres real (Docker), Spotless, Checkstyle, PMD, SpotBugs, JaCoCo ≥ 80%
```

Relatório de cobertura em `target/jacoco-report/index.html`. O CI
([`.github/workflows/ci.yml`](.github/workflows/ci.yml)) roda Gitleaks, Semgrep, `verify` e
SonarCloud com quality gate bloqueante em todo PR.
