---
name: implement-hexagonal-slice
description: Implementar ou alterar um caso de uso ponta a ponta no oficina-billing, de dentro para fora (agregado → porta → serviço → adaptadores → migration → testes), respeitando o contrato hexagonal. Use ao criar um endpoint, um comando de domínio ou um adaptador novo.
---

# Implementar uma fatia hexagonal

Procedimento para uma fatia vertical. Por que existe: no `oficina-app`, a ordem "DTO primeiro,
serviço com setters depois" produziu o `ServiceItem` anêmico que o harness proíbe. Começar pelo
agregado inverte isso. Regras e justificativas estão em
`docs/architecture/hexagonal-contract.md` e `docs/architecture/domain-decisions.md`; aqui só a
sequência e os arquivos.

## Antes

1. Nomeie a fatia pela intenção de negócio (ex.: "registrar peça", "alterar preço do serviço").
2. Confira no `CONTEXT.md` cada termo que vai virar nome de classe ou método.
3. Se a fatia expõe rota, abra `docs/architecture/rest-contracts.md` e execute
   `check-contract-parity` antes de escrever o DTO.
4. Liste os arquivos abaixo que a fatia cria ou toca e coloque a lista no plano (passo 4 do
   `docs/agents/workflow.md`).

## Sequência (um passo, um teste verde, próximo passo)

Pacote base: `br.com.fiap.postech.soat16.billing.<contexto>`.

| # | Arquivo | O que fazer | Teste que fecha o passo |
|---|---|---|---|
| 1 | `domain/exception/<Agregado>ErrorCode.java` | Enum `implements ErrorCode` com os códigos da fatia (`INVALID_PART`, `PART_NOT_FOUND`) | — |
| 2 | `domain/exception/<Nome>Exception.java` | `extends AppException`, construtor com mensagem em inglês + código + `ErrorType` | — |
| 3 | `domain/model/<Agregado>.java` | Factory `register(...)` que valida **todos** os invariantes; método de comportamento da fatia; `restore(...)` para reidratação; `equals`/`hashCode` por id | `domain/model/<Agregado>Test.java`: um teste por invariante (válido + violação com a exceção e o código), um por comportamento |
| 4 | `application/port/out/<Agregado>PersistencePort.java` | Só os métodos que a fatia precisa, `Uni<T>`, Javadoc dizendo que `null` = não encontrado | — |
| 5 | `application/command/<Intencao><Agregado>Command.java` (ou `*Query`) | Record com os dados de entrada, sem lógica | — |
| 6 | `application/result/<Agregado>Result.java` | Record com `static from(<Agregado>)` | — |
| 7 | `application/<Agregado>Service.java` | `@ApplicationScoped @RequiredArgsConstructor`; método do caso de uso com `@WithSession`/`@WithTransaction`; carrega → delega ao agregado → persiste → `Result.from` | `application/<Agregado>ServiceTest.java` com a porta `@Mock` |
| 8 | `src/main/resources/db/migration/V<n>__<descricao>.sql` | DDL completo da tabela/alteração (skill `add-flyway-migration`) | — |
| 9 | `adapter/out/persistence/entity/<Agregado>JpaEntity.java` | Colunas espelhando a migration | — |
| 10 | `adapter/out/persistence/mapper/<Agregado>PersistenceMapper.java` | `toDomain` via `restore`, `toJpaEntity`, `copyState` | `...mapper/<Agregado>PersistenceMapperTest.java` |
| 11 | `adapter/out/persistence/<Agregado>Repository.java` | `PanacheRepositoryBase<JpaEntity, UUID>` + implementa a porta | `adapter/out/persistence/<Agregado>RepositoryIT.java` (skill `write-postgres-it`) |
| 12 | `adapter/in/rest/dto/request/<Agregado>RequestDto.java` | Record com Bean Validation e **as mensagens do contrato** | — |
| 13 | `adapter/in/rest/dto/response/<Agregado>ResponseDto.java` | Record; `@JsonInclude(NON_NULL)` se o contrato tiver | — |
| 14 | `adapter/in/rest/mapper/<Agregado>RestMapper.java` | `final`, construtor privado, `toXxxCommand`, `toResponse` | `...mapper/<Agregado>RestMapperTest.java` |
| 15 | `adapter/in/rest/openapi/<Agregado>ControllerDocs.java` | Interface com `@Path`, verbos, `@APIResponse` de **cada** status (inclusive 401/403), `@SecurityRequirement`, `@Tag`, `@Valid` | — |
| 16 | `adapter/in/rest/controller/<Agregado>Controller.java` | `@ApplicationScoped @RequiredArgsConstructor @RolesAllowed(...)`; implementa Docs; só mapeia | `...controller/<Agregado>ControllerTest.java` com o `Service` `@Mock` |
| 17 | — | — | `adapter/in/rest/controller/<Agregado>ControllerIT.java` e `adapter/in/rest/openapi/<Agregado>OpenApiContractIT.java` (skill `write-postgres-it`) |

Para uma fatia que **altera** algo existente, pule o que já existe e toque só o necessário — mas
mantenha a ordem (agregado antes de serviço, serviço antes de controller).

## Depois

- `./mvnw --batch-mode verify` completo; leia a saída até o fim.
- Checklist de revisão do passo 8 do `docs/agents/workflow.md`.
- Se um termo novo entrou, `CONTEXT.md`; se um padrão mudou, o documento correspondente em `docs/`.

## Sinais de que a fatia saiu do trilho

- O `Service` tem um `if` que lê estado do agregado → o `if` é um método do agregado.
- O DTO tem validação que o agregado não tem → copie a invariante para o agregado.
- Precisou de `set...` público no agregado → é um método de comportamento com nome de negócio.
- O controller importa algo de `adapter.out` ou de `domain.model` com estado → volte ao mapper.
- Criou uma interface com uma única implementação que não é porta → apague a interface.
- Criou uma classe `*Helper`/`*Util`/`*Manager` → ela pertence ao agregado ou ao serviço.
