# Diagnóstico do `oficina-app`: o que preservar, o que melhorar, o que não transportar

Levantamento feito sobre o repositório `oficina-app` (branch `main`, novembro de 2026) com foco nos
contextos que migram para o Billing (`servicecatalog`, `part`) e no `shared`. Cada afirmação aponta
o arquivo que a sustenta. O objetivo não é julgar o monólito, mas impedir que agentes copiem padrões
pelo simples fato de existirem.

Caminhos abaixo são relativos a `oficina-app/src/main/java/br/com/fiap/postech/soat16/fase1/` e
`oficina-app/src/test/java/br/com/fiap/postech/soat16/fase1/`, salvo indicação.

## 1. O que preservar (decisões maduras, com evidência)

| Padrão | Evidência | Por que fica |
|---|---|---|
| Pacote por bounded context, camadas hexagonais dentro de cada um | `AGENTS.md` do monólito; qualquer contexto, p. ex. `servicecatalog/{domain,application,adapter}` | Dá ao agente um lugar óbvio para cada classe e permite regra de dependência verificável |
| Porta de saída em `application/port/out`, implementada em `adapter/out` | `servicecatalog/application/port/out/ServiceCatalogPersistencePort.java` ← `servicecatalog/adapter/out/persistence/ServiceItemRepository.java` | Inversão de dependência de fato: o serviço depende da interface, o repositório Panache a implementa |
| Lookup entre contextos por porta dedicada, implementada por adaptador do contexto consumidor | `vehicle/application/port/out/VehicleCustomerLookupPort.java` ← `vehicle/adapter/out/customer/CustomerLookupAdapter.java` | Mantém o contexto consumidor dono do seu contrato; é o padrão que o orçamento usará para consultar o catálogo |
| Interface `*ControllerDocs` com JAX-RS + OpenAPI, controller só implementa | `servicecatalog/adapter/in/rest/openapi/ServiceItemControllerDocs.java` e `.../controller/ServiceItemController.java` | Separa contrato publicado de implementação; o controller fica legível |
| `Command`/`Query`/`Result` como records; mappers REST e de persistência como utilitários estáticos finais | `servicecatalog/application/command/*`, `.../result/ServiceItemResult.java`, `.../adapter/in/rest/mapper/ServiceCatalogRestMapper.java` | Fronteiras explícitas entre DTO, caso de uso e domínio, sem framework de mapeamento |
| Modelo de erro estruturado `AppException` + `ErrorCode` + `ErrorType` → `AppExceptionMapper` → `ApiErrorResponseDto` | `shared/domain/exception/AppException.java`, `shared/adapter/in/rest/exception/AppExceptionMapper.java`, `shared/adapter/in/rest/dto/ApiErrorResponseDto.java` | Código de erro estável para o cliente, mapeamento único `ErrorType → status` |
| RBAC por `@RolesAllowed` na classe (leitura) e no método (escrita), com `deny-unannotated-endpoints: true` | `ServiceItemController` (`{"ADMIN","MECHANIC"}` na classe, `"ADMIN"` em `create/update/delete`); `application.yaml` | Nenhuma rota nasce aberta por esquecimento |
| JWT RS256 por SmallRye, issuer `oficina-api`, chave fora do build | `application.yaml` (`mp.jwt.verify.*`), `auth/adapter/in/startup/JwtKeyStartupGuard.java`, `architecture/JwtKeyPackagingTest.java`, `.gitleaksignore` | O Billing valida com a chave pública desse mesmo par |
| `*IT` com `@QuarkusTest` + Postgres real via `PostgresTestResource` + par RS256 efêmero via `JwtKeyPairTestResource` | `shared/test/infrastructure/PostgresTestResource.java`, `.../JwtKeyPairTestResource.java`, `workorder/adapter/in/rest/controller/WorkOrderControllerIT.java` | Teste de borda que atravessa controller, caso de uso, domínio e banco; token cunhado pelo teste |
| Teste do contrato OpenAPI publicado | `workorder/adapter/in/rest/openapi/WorkOrderOpenApiContractIT.java` | Garante que o Swagger diz a verdade sobre status e segurança |
| `@Nested` + `@DisplayName` em português nos testes | Qualquer `*Test`/`*IT` | Relatório de teste legível para a banca e para revisão |
| Gates estáticos no `verify`: Spotless, Checkstyle (140 colunas), PMD + CPD, SpotBugs, JaCoCo 80% de linhas | `pom.xml`, `config/checkstyle/checkstyle.xml`, `config/pmd/ruleset.xml` | Mesma régua para pessoa e agente, num único comando |
| Gitleaks no histórico e Semgrep `p/ci` no CI | `.github/workflows/ci.yml` | Baratos e já calibrados |
| Management na porta 9000 (health, métricas), OpenAPI na 8080, logs JSON, OTel opcional, `X-Request-ID` | `application.yaml`, `shared/adapter/in/rest/RequestCorrelationFilter.java`, `k8s/base/deployment.yaml` (probes) | Observabilidade já integrada à stack da Fase 3 |
| Container non-root, sem chave na imagem, imagem imutável por SHA | `infra/docker/Dockerfile`, `.github/workflows/deploy.yml` | Postura de segurança já validada |
| Glossário `CONTEXT.md` + ADRs curtos em `docs/adr/` + `docs/agents/{domain,issue-tracker}.md` | raiz e `docs/` do monólito | Já é o formato que os agentes do time consomem |

## 2. O que melhorar ao transportar

| Observação | Evidência | O que o Billing faz |
|---|---|---|
| Os testes de arquitetura varrem imports como texto, contexto a contexto, com listas de marcadores (`".dto."`, `".repository."`) | `architecture/HexagonalDependencyArchitectureTest.java`, `servicecatalog/architecture/ServiceCatalogArchitectureTest.java` | ArchUnit sobre bytecode, regras por padrão de pacote válidas para qualquer contexto novo sem copiar teste ([executable-rules.md](executable-rules.md)) |
| Os `*IT` **não rodam no CI**: `skipITs=true` no pom, Surefire inclui só `*Test`, e `ci.yml` executa `./mvnw verify` sem `-Pitest`. A cobertura de 80% é medida só com unitários | `pom.xml` (`<skipITs>true</skipITs>`, perfil `itest`), `.github/workflows/ci.yml` | Failsafe roda `*IT` no `verify` por padrão; cobertura soma unitários e integração ([quality-gates.md](../engineering/quality-gates.md)) |
| Não existe `*IT` de HTTP para o catálogo; só `*RepositoryIT` e controller unitário com mock. Os erros das rotas do catálogo nunca foram fixados por teste | `servicecatalog/adapter/out/persistence/ServiceItemRepositoryIT.java`, `.../controller/ServiceItemControllerTest.java`; ausência de `ServiceItemControllerIT` | Cada rota do catálogo ganha `*IT` de sucesso e de erro ([rest-contracts.md](rest-contracts.md)) |
| Sonar configurado mas nunca executado (`sonar-project.properties` aponta para SonarQube de homelab; o job foi removido do CI) e com DTOs excluídos de análise e cobertura | `sonar-project.properties`, comentário no topo de `ci.yml` | SonarCloud via Maven com `qualitygate.wait=true`; sem exclusão de cobertura |
| Auditoria grava `createdBy = "system"` fixo | `shared/adapter/out/persistence/AuditJpaEntityListener.java` | [RECOMENDADO] preencher com o `upn` do JWT quando houver identidade; caso contrário manter `system` |
| Dois Hibernate na mesma aplicação (`quarkus-hibernate-orm-panache` e `quarkus-hibernate-reactive-panache`) com o bloqueante sem uso | `pom.xml` | Só o reativo ([ADR-0001](../adr/0001-hibernate-reactive-e-mutiny-por-paridade-com-o-os.md)) |
| `LocalDateTime` em `Part` e `OffsetDateTime` em `ServiceItem` para o mesmo conceito (`createdAt`) | `part/domain/model/Part.java`, `shared/domain/model/audit/AuditableEntity.java` | [JUSTIFICAR] padronizar em `OffsetDateTime` UTC; altera o formato JSON de `createdAt` das peças (ver [rest-contracts.md](rest-contracts.md)) |

## 3. O que não transportar

| Padrão do monólito | Evidência | Por que não | Regra no Billing |
|---|---|---|---|
| **Entidade anêmica** no catálogo: `@Getter @Setter @AllArgsConstructor @NoArgsConstructor`, estado aplicado pelo serviço via `apply(...)` com setters | `servicecatalog/domain/model/ServiceItem.java`, `ServiceItemService.apply(...)` | Contradiz o próprio `AGENTS.md` do monólito; invariantes (preço > 0) só existem no DTO | Domínio rico: factory + comportamento + invariante; sem setter público ([domain-decisions.md](domain-decisions.md)) |
| **Dois modelos de erro paralelos**: `BusinessException`/`ResourceNotFoundException` (RuntimeException puras) → `GlobalExceptionMapper` com corpo `Map` `{status,error,message,timestamp}`, ao lado de `AppException` → `ApiErrorResponseDto`. O catálogo usa o legado; `ServiceItemNotFoundException` (moderno) existe mas o `ServiceItemService` não a usa | `shared/domain/exception/{BusinessException,ResourceNotFoundException}.java`, `shared/adapter/in/rest/exception/GlobalExceptionMapper.java`, `ServiceItemService.notFound(...)`, `servicecatalog/domain/exception/ServiceItemNotFoundException.java` | Dois formatos de erro para o mesmo cliente; exceção sem código estável | Só `AppException` ([ADR-0003](../adr/0003-modelo-unico-de-erro-appexception.md)) |
| **Flyway nominal**: `V1__create_schema.sql` tem uma linha (`CREATE SCHEMA`); as tabelas são criadas por `hibernate-orm.schema-management.strategy: update`; no perfil de teste, `drop-and-create` | `src/main/resources/db/migration/V1__create_schema.sql`, `application.yaml` | Schema de produção definido por efeito colateral do ORM; migration não representa o banco | Flyway cria tabelas; Hibernate `validate` em todo perfil ([ADR-0002](../adr/0002-flyway-dono-do-schema-e-hibernate-validate.md)) |
| **Estoque dentro de `Part`**: `stockQuantity`, `minimumStock`, `isLowStock`, `decreaseStock`, `increaseStock`, rotas `GET /admin/parts/low-stock` e `PATCH /admin/parts/{id}/stock` | `part/domain/model/Part.java`, `part/adapter/in/rest/openapi/PartControllerDocs.java` | A spec #106 dá o saldo ao Execution | `Part` do Billing não tem campo de quantidade; as duas rotas não existem aqui ([rest-contracts.md](rest-contracts.md)) |
| Perfil `%test` ligando DevServices e trocando a estratégia de schema | `application.yaml` (`"%test"`) | Teste rodando contra schema diferente do real | `%test` não toca banco nem schema; `PostgresTestResource` injeta a URL |
| Constante pública com nome em português para texto de erro em inglês (`PECA_INSUMO = "Part/Supply"`) | `part/application/PartService.java` | Mistura de idiomas no código | Inglês no código; mensagens de erro são as do contrato |
| Repositório devolvendo `null` em `Uni` para "não encontrado" e o serviço convertendo com `onItem().ifNull()` | `ServiceItemRepository.findServiceItemById`, `ServiceItemService.findById` | É idiomático em Mutiny, mas o contrato da porta fica implícito | [RECOMENDADO] manter o idioma Mutiny, documentando na porta que o item pode ser nulo; não introduzir `Optional<Uni>` nem `Uni<Optional>` sem necessidade |
| Artefatos soltos na raiz (`.scratch/`, `dependency-check-report.zip`, `WORKORDER.md`) | raiz do monólito | Ruído para quem lê e para quem indexa | Raiz contém só `AGENTS.md`, `CONTEXT.md`, `README.md`, build e `docs/` |

## 4. Fatos do contrato do catálogo que o Billing precisa honrar

Extraídos de código e da collection Postman (`postman/Oficina-Mecanica-E2E.postman_collection.json`),
que é o único cliente automatizado dessas rotas e **afirma apenas status HTTP e alguns campos**
(`id`, `unitPrice`, `basePrice`, `stockQuantity` no ajuste de estoque). O detalhe completo está em
[rest-contracts.md](rest-contracts.md).

- Rotas sem prefixo `/v1`: `@Path("/admin/services")` e `@Path("/admin/parts")`.
- Leitura para `ADMIN` e `MECHANIC`; escrita só `ADMIN`; `201` com `Location: /admin/services/{id}`.
- `404` do catálogo hoje sai pelo mapper legado (`{status:404, error:"Not Found", message:"Service not found with id: <id>"}` e `"Part/Supply not found with id: <id>"`).
- `400` de validação sai por `ValidationExceptionMapper` (`{status:400, error:"Validation Error", violations:["campo: mensagem"], timestamp}`), com mensagens em inglês definidas nos DTOs.
- `401`/`403` são a resposta padrão do Quarkus, sem corpo definido pelo projeto (`auth/adapter/in/rest/SecurityIT.java` afirma só o status).
- Não existe hoje nenhuma fonte de `409` no catálogo (sem unicidade de nome, sem conflito de versão exposto).
