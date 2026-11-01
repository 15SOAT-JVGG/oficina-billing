# AGENTS.md — oficina-billing

Ponto de entrada para agentes que implementam este serviço. Leia este arquivo inteiro antes de
qualquer tarefa; ele diz o que é obrigatório e onde está o detalhe. Não repita aqui o que já está nos
documentos referenciados: quando precisar de mais, siga o link.

## O serviço

`oficina-billing` é o serviço de **Billing** da oficina mecânica (FIAP 16SOAT — Tech Challenge,
Fase 4, [spec #106](https://github.com/15SOAT-JVGG/oficina-app/issues/106)). Nasce com o
**catálogo de serviços e peças com preço**, extraído do monólito
[`oficina-app`](https://github.com/15SOAT-JVGG/oficina-app)
([issue #107](https://github.com/15SOAT-JVGG/oficina-app/issues/107)), preservando as rotas e os
erros que os clientes já conhecem. Orçamento, decisão do cliente e cobrança (Mercado Pago) chegam em
issues posteriores; **saldo de estoque não é deste serviço** (é do Execution).

O `oficina-app` é a referência de padrões, não um modelo a copiar. O que vale, o que foi melhorado e o
que não deve ser transportado está em [`docs/architecture/legacy-diagnosis.md`](docs/architecture/legacy-diagnosis.md).

## Como ler as regras

Todo documento deste repositório classifica suas afirmações com um destes rótulos:

| Rótulo | Significado |
|---|---|
| **[OBRIGATÓRIO]** | Regra verificada por teste, build ou revisão. Não se negocia na implementação; muda-se por ADR. |
| **[RECOMENDADO]** | Padrão esperado. Desviar é aceitável se a alternativa for mais simples e o PR explicar por quê. |
| **[JUSTIFICAR]** | Decisão que exige uma nota no PR ou um ADR em `docs/adr/` antes de ser aplicada. |
| **[EVITAR]** | Prática observada no monólito ou comum em geração automática que não entra aqui. |

## Stack

| Item | Versão / Tecnologia |
|---|---|
| Linguagem | Java 21 |
| Framework | Quarkus 3.32.3 (`quarkus-bom`), mesmo do `oficina-app` |
| Build | Maven, sempre pelo wrapper `./mvnw` |
| Persistência | Hibernate Reactive + Panache, PostgreSQL 16, banco e schema próprios (`oficina_billing`) |
| Migrations | Flyway, único dono do schema; Hibernate em `validate` |
| Assíncrono | Mutiny (`Uni<T>`) nas camadas de aplicação e adaptadores; domínio síncrono |
| Segurança | SmallRye JWT RS256 — **só validação** com a chave pública do OS Service; RBAC por `@RolesAllowed` |
| Documentação | SmallRye OpenAPI / Swagger UI |
| Testes | JUnit 5, Mockito, REST-assured, Testcontainers (Postgres real), ArchUnit |
| Qualidade | JaCoCo (≥ 80% linhas), Checkstyle, PMD, SpotBugs, Spotless, SonarCloud (gate bloqueante) |

## Comandos

`pom.xml`, wrapper, configuração e testes de arquitetura seguem
[`docs/engineering/quality-gates.md`](docs/engineering/quality-gates.md) e
[`docs/architecture/executable-rules.md`](docs/architecture/executable-rules.md):

```shell
./mvnw test                 # unitários e testes de arquitetura (*Test)
./mvnw verify               # gate completo: unitários + integração (*IT, requer Docker) + qualidade estática + cobertura
./mvnw verify -DskipITs     # ciclo local rápido sem Docker; o gate de cobertura pode reprovar, e isso é esperado
./mvnw quarkus:dev          # dev mode (requer Postgres local, ver docs/engineering/persistence.md)
```

**[OBRIGATÓRIO]** Uma tarefa só está concluída depois de `./mvnw verify` executado com sucesso e a
saída registrada como evidência. Ver [`docs/agents/workflow.md`](docs/agents/workflow.md).

## Regras que não se negociam

1. **Arquitetura hexagonal por bounded context.** Pacote raiz `br.com.fiap.postech.soat16.billing`;
   primeiro nível é o contexto (`catalog`, `shared`), segundo é a camada (`domain`, `application`,
   `adapter.in`, `adapter.out`). `domain` não conhece ninguém; `application` conhece `domain` e as
   próprias portas; `adapter` implementa portas. Contrato e dependências permitidas em
   [`docs/architecture/hexagonal-contract.md`](docs/architecture/hexagonal-contract.md).
2. **Domínio rico.** Regra de negócio e invariante vivem no agregado (factory method nomeado pela
   intenção, métodos de comportamento, exceção de domínio). Sem setters públicos, sem Lombok que gere
   estado mutável, sem Mutiny, JPA ou Jackson no domínio.
   [`docs/architecture/domain-decisions.md`](docs/architecture/domain-decisions.md).
3. **Um único modelo de erro.** Toda exceção de domínio deriva de `AppException` com `ErrorCode` e
   `ErrorType`; a borda REST a traduz em `ApiErrorResponseDto`. O par legado
   `BusinessException`/`ResourceNotFoundException` do monólito não é portado.
   [ADR-0003](docs/adr/0003-modelo-unico-de-erro-appexception.md).
4. **Contratos preservados.** Rotas, verbos, status HTTP, DTOs e mensagens de validação do catálogo
   são os do `oficina-app`, documentados em
   [`docs/architecture/rest-contracts.md`](docs/architecture/rest-contracts.md). Qualquer
   divergência é [JUSTIFICAR].
5. **Flyway é o schema.** Toda tabela nasce numa migration versionada; o Hibernate só valida.
   [ADR-0002](docs/adr/0002-flyway-dono-do-schema-e-hibernate-validate.md).
6. **JWT só se valida.** Nenhuma chave privada no repositório, na imagem ou na configuração;
   `quarkus-smallrye-jwt-build` apenas em escopo de teste.
   [`docs/engineering/security.md`](docs/engineering/security.md).
7. **Testes nos dois níveis.** `*Test` unitário com Mockito para domínio, serviço, mappers e
   controller; `*IT` com `@QuarkusTest` + Postgres real via Testcontainers para cada rota, cobrindo
   sucesso e `400`, `401`, `403`, `404` (e `409`/`422` quando existirem). Os `*IT` fazem parte do
   `verify` e contam para a cobertura. [`docs/engineering/testing.md`](docs/engineering/testing.md).
8. **Regras arquiteturais são testes.** Direção de dependências, forma dos pacotes, nomenclatura,
   pureza do domínio, convenções de teste, contrato da configuração e do build são verificados por
   ArchUnit e testes de contrato descritos em
   [`docs/architecture/executable-rules.md`](docs/architecture/executable-rules.md). Uma regra que
   falha se corrige movendo código, não afrouxando a regra.
9. **Nenhuma verificação é declarada sem ter sido executada.** Se algo não pôde rodar (sem Docker,
   sem rede, sem credencial), o PR diz isso explicitamente.

## Linguagem

Nomes de código em inglês, com o vocabulário do negócio: `ServiceItem`, `Part`, `basePrice`,
`unitPrice`. Em português apenas o texto voltado a pessoas: `@DisplayName`, mensagens de commit, PR,
documentação. O glossário é [`CONTEXT.md`](CONTEXT.md); termo que não está lá e não está no
`oficina-app` é sinal de invenção — pare e pergunte.

## Mapa da documentação

| Quero… | Leia |
|---|---|
| Saber o que o monólito faz bem, mal, e o que não copiar | [`docs/architecture/legacy-diagnosis.md`](docs/architecture/legacy-diagnosis.md) |
| Organizar pacotes, portas e adaptadores | [`docs/architecture/hexagonal-contract.md`](docs/architecture/hexagonal-contract.md) |
| Modelar um agregado, VO ou invariante | [`docs/architecture/domain-decisions.md`](docs/architecture/domain-decisions.md) |
| Expor ou alterar uma rota | [`docs/architecture/rest-contracts.md`](docs/architecture/rest-contracts.md) |
| Saber quais regras os testes de arquitetura cobram | [`docs/architecture/executable-rules.md`](docs/architecture/executable-rules.md) |
| Escrever código legível e coeso | [`docs/engineering/clean-code.md`](docs/engineering/clean-code.md) |
| Escrever testes | [`docs/engineering/testing.md`](docs/engineering/testing.md) |
| Mexer em banco, transação ou migration | [`docs/engineering/persistence.md`](docs/engineering/persistence.md) |
| Mexer em autenticação ou autorização | [`docs/engineering/security.md`](docs/engineering/security.md) |
| Entender o build, a cobertura, o Sonar e o CI | [`docs/engineering/quality-gates.md`](docs/engineering/quality-gates.md) |
| Executar uma issue do começo ao fim | [`docs/agents/workflow.md`](docs/agents/workflow.md) |
| Ler, comentar ou fechar issues | [`docs/agents/issue-tracker.md`](docs/agents/issue-tracker.md) |
| Usar o glossário e os ADRs | [`docs/agents/domain.md`](docs/agents/domain.md) |

## Skills

Procedimentos repetíveis com passo a passo próprio vivem em `.agents/skills/`. Use-os quando a
tarefa casar com a descrição; eles referenciam os documentos acima em vez de duplicá-los.

| Skill | Quando |
|---|---|
| `implement-hexagonal-slice` | Criar ou alterar um caso de uso ponta a ponta (domínio → porta → serviço → adaptadores → testes) |
| `write-postgres-it` | Escrever um `*IT` de rota com Postgres real, token JWT e asserções de erro |
| `check-contract-parity` | Conferir uma rota do Billing contra a rota equivalente do `oficina-app` |
| `add-flyway-migration` | Criar ou alterar tabela com migration versionada e Hibernate em `validate` |
