# Um único modelo de erro: `AppException` → `ApiErrorResponseDto`

Toda exceção de negócio do Billing estende `AppException` (`shared.domain.exception`) carregando um
`ErrorCode` do contexto e um `ErrorType` (`VALIDATION`, `UNAUTHORIZED`, `NOT_FOUND`, `CONFLICT`,
`GONE`, `BUSINESS`); o `AppExceptionMapper` traduz o tipo em status HTTP e devolve
`ApiErrorResponseDto` (`status`, `type`, `code`, `message`, `path`, `traceId`, `timestamp`). O
`oficina-app` mantém dois modelos em paralelo — este e o par legado
`BusinessException`/`ResourceNotFoundException`, traduzido por `GlobalExceptionMapper` num `Map`
sem código — e o catálogo usa o legado. O par legado não é portado.

A consequência visível é que o `404` do catálogo muda de corpo (o status é o mesmo): deixa de ser
`{status, error, message, timestamp}` e passa a ser o `ApiErrorResponseDto` com
`code: SERVICE_ITEM_NOT_FOUND` / `PART_NOT_FOUND`, formato que o próprio monólito já emite para
serviço não encontrado nos fluxos de Ordem de Serviço. Os clientes automatizados conhecidos (Postman,
Swagger) afirmam só o status. O `400` de Bean Validation mantém o corpo do `ValidationExceptionMapper`
do monólito, porque ele é parte do contrato das rotas e nada ganha em mudar.

## Considered Options

**Portar os dois modelos para preservar o corpo do `404` byte a byte.** Rejeitado: perpetua uma
inconsistência que o monólito só não corrigiu por custo de migração, e deixa o Billing sem código de
erro estável justamente nas rotas que o orçamento vai consumir.

## Consequences

Decisão reversível antes do merge de #107 se o time preferir paridade byte a byte: bastaria um mapper
dedicado para `NOT_FOUND`. Ela está listada como pendente de confirmação em
[`docs/architecture/rest-contracts.md`](../architecture/rest-contracts.md). O mapper de fallback
(`Throwable` → `500`) e o de `ConstraintViolationException` (→ `400`) completam o conjunto; nenhum
outro `ExceptionMapper` é necessário.
