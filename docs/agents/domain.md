# Documentação de domínio: como consumir e como manter

## Antes de explorar o código

Leia [`CONTEXT.md`](../../CONTEXT.md) na raiz e os ADRs de `docs/adr/` que tocam a área da tarefa.
Este repositório é **single-context** (um `CONTEXT.md`, um `docs/adr/`); se vier a ter mais de um
contexto de negócio com vocabulário próprio, passa a existir um `CONTEXT-MAP.md` na raiz apontando
para um `CONTEXT.md` por contexto, no formato que a skill `domain-modeling` do time descreve.

O glossário do `oficina-app` (`oficina-app/CONTEXT.md`) é a referência para termos que ainda não
chegaram ao Billing (Ordem de Serviço, orçamento, decisão, cobrança). Quando um deles entrar aqui, a
definição é copiada — não reescrita — e adaptada só no que o Billing acrescenta.

## Use o vocabulário do glossário

Em nome de classe, método, teste, commit, comentário de issue e documento: o termo é o do
`CONTEXT.md`, não um sinônimo. Os `_Avoid_` do glossário existem para isso.

Termo que você precisa e não está no glossário é um sinal. Ou está inventando linguagem que o
projeto não usa (reconsidere), ou há uma lacuna real: proponha a definição no PR e acrescente ao
`CONTEXT.md` no mesmo PR, sem detalhes de implementação (o glossário define o que a coisa **é**).

## ADRs

Formato: `docs/adr/NNNN-slug-em-portugues.md`, numeração sequencial, título curto, um a três
parágrafos ("contexto, decisão, porquê"), com `## Considered Options` e `## Consequences` só quando
acrescentam algo. É o formato dos ADRs do `oficina-app` e dos três que já existem aqui.

Um ADR só se justifica quando as três condições valem:

1. **difícil de reverter** — mudar de ideia depois custa;
2. **surpreendente sem contexto** — quem ler o código vai perguntar "por que assim?";
3. **resultado de trade-off real** — havia alternativa razoável e ela foi rejeitada por motivo específico.

Faltando uma, registre a decisão como nota no PR ou como parágrafo em
`docs/architecture/domain-decisions.md`.

## Conflito com ADR existente

Se a tarefa contradiz um ADR (daqui ou do `oficina-app`, que a spec #106 já lista como conflitantes:
ADR-0004, ADR-0005, ADR-0007), não ignore nem contorne: nomeie o conflito no PR
("Contradiz o ADR-000N porque …") e, se a decisão nova vencer, escreva o ADR substituto com
`superseded by` no antigo.
