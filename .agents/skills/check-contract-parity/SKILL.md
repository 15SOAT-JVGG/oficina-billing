---
name: check-contract-parity
description: Conferir uma rota do oficina-billing contra a rota equivalente do oficina-app (caminho, verbo, papéis, status, DTOs, mensagens de validação, corpo de erro) e produzir um relatório de paridade para o PR. Use antes de criar ou alterar qualquer rota do catálogo.
---

# Conferir paridade de contrato com o `oficina-app`

Por que existe: a issue #107 exige "mesmas rotas e mesmos erros", e o único cliente automatizado
(Postman) afirma pouco. O que vale como contrato está escrito em
`docs/architecture/rest-contracts.md`; esta skill confere o código do Billing contra esse documento
**e** contra o código-fonte do monólito, e registra o resultado.

## Entradas

- Rota (ou controller) do Billing a conferir.
- Clone do `oficina-app` acessível (assumido em `../oficina-app`; ajuste o caminho).

## Passos

1. **Localize a fonte no monólito.** Para o catálogo:
   - `src/main/java/br/com/fiap/postech/soat16/fase1/servicecatalog/adapter/in/rest/{openapi/ServiceItemControllerDocs,controller/ServiceItemController,dto/request/ServiceItemRequestDto,dto/response/ServiceItemResponseDto}.java`
   - `src/main/java/br/com/fiap/postech/soat16/fase1/part/adapter/in/rest/{openapi/PartControllerDocs,controller/PartController,dto/request/PartRequestDto,dto/response/PartResponseDto}.java`
   - `src/main/java/br/com/fiap/postech/soat16/fase1/shared/adapter/in/rest/exception/{ValidationExceptionMapper,GlobalExceptionMapper,AppExceptionMapper}.java`
   - `postman/Oficina-Mecanica-E2E.postman_collection.json` (pastas `04 - Parts and Supplies`, `05 - Service Catalog`)

2. **Extraia, lado a lado, por operação**, numa tabela com colunas `oficina-app | Billing | igual?`:
   - `@Path` da classe e do método, verbo;
   - `@RolesAllowed` efetivo (classe + método);
   - status de sucesso e `Location`;
   - `@APIResponse` documentados;
   - campos do `*RequestDto` com anotações e **texto exato** das mensagens;
   - campos do `*ResponseDto` com tipos (atenção a `LocalDateTime` × `OffsetDateTime`);
   - corpo do `400` (`ValidationExceptionMapper`), do `404` (legado × `ApiErrorResponseDto`).

   Comandos úteis (a partir da raiz do Billing):
   ```shell
   rg -n '@Path|@GET|@POST|@PUT|@DELETE|@PATCH|@RolesAllowed|responseCode' ../oficina-app/src/main/java/br/com/fiap/postech/soat16/fase1/servicecatalog/adapter/in/rest/
   rg -n 'message = "' ../oficina-app/src/main/java/br/com/fiap/postech/soat16/fase1/servicecatalog/adapter/in/rest/dto/
   rg -n '@Path|@GET|@POST|@PUT|@DELETE|@PATCH|@RolesAllowed|responseCode' src/main/java/br/com/fiap/postech/soat16/billing/catalog/adapter/in/rest/
   ```

3. **Classifique cada diferença**:
   - **Prevista** — já está em `rest-contracts.md` como [JUSTIFICAR] com justificativa (ex.: campos de
     estoque removidos, corpo do `404`, `OffsetDateTime`). Cite a seção.
   - **Pendente** — está em `domain-decisions.md` como pendência sem resposta. Bloqueia o merge até
     resposta registrada.
   - **Não prevista** — não está em documento algum. Ou é erro de implementação (corrija) ou é
     decisão nova (pare, registre na issue, atualize `rest-contracts.md` no mesmo PR se aprovada).

4. **Confira o Postman**: para cada request das pastas do catálogo, o status esperado e os campos
   afirmados (`id`, `unitPrice`, `basePrice`) continuam válidos contra o Billing. Requests de
   `low-stock` e `stock` não se aplicam (são do Execution) — diga isso no relatório.

5. **Confira o OpenAPI servido** (com a aplicação de pé ou pelo `*OpenApiContractIT`): os status
   documentados por operação batem com a tabela de `rest-contracts.md`.

## Saída

Cole no PR, na seção "Verificações executadas":

```markdown
### check-contract-parity — <rota ou controller>
| Operação | Item | oficina-app | Billing | Status |
|---|---|---|---|---|
| POST /admin/parts | stockQuantity no request | @NotNull | ausente (ignorado) | Prevista: rest-contracts.md § PartRequestDto |
| GET /admin/parts/{id} | corpo 404 | {status,error,message} | ApiErrorResponseDto | Prevista: ADR-0003 (confirmar) |
| ... | ... | ... | ... | ... |
Divergências não previstas: nenhuma / <lista>
Postman: N requests aplicáveis, todas compatíveis; low-stock e stock fora do Billing.
```

Se houver "não prevista" sem resolução, o PR fica em rascunho.
