# Billing

Serviço que detém o catálogo de serviços e peças da oficina com os seus preços e, nas fases seguintes,
o orçamento, a decisão do cliente e a cobrança. Este glossário cobre o que existe hoje: o catálogo.
Termos de orçamento e cobrança entram quando as issues correspondentes forem implementadas; até lá,
o vocabulário de referência é o `CONTEXT.md` do `oficina-app`.

## Language

### Catálogo

**Catálogo**:
O conjunto de serviços e peças que a oficina oferece, cada um com o seu preço de referência. É
mantido pelo ADMIN e consultado pelo MECHANIC.
_Avoid_: tabela de preços, cadastro

**Serviço** (`ServiceItem`):
Mão de obra oferecida pela oficina, com preço base e duração estimada em minutos. Pode estar ativo ou
inativo; inativar não apaga o registro.
_Avoid_: service, item de serviço, mão de obra (como entidade)

**Peça** (`Part`):
Item físico do catálogo aplicado em uma Ordem de Serviço, com preço unitário e unidade de medida.
Classifica-se em peça propriamente dita ou insumo.
_Avoid_: produto, material, estoque

**Insumo** (`PartType.SUPPLY`):
Peça de consumo não individualizável (óleo, graxa, fluido). É o outro valor de `PartType`, ao lado de
`PART`.
_Avoid_: consumível, suprimento

**Preço base** (`basePrice`):
Valor de referência de um serviço no catálogo, no momento da consulta.
_Avoid_: valor, custo

**Preço unitário** (`unitPrice`):
Valor de referência de uma unidade de uma peça no catálogo, no momento da consulta.
_Avoid_: preço, valor

**Unidade** (`unit`):
Como a peça é contada no catálogo e no orçamento: `UN`, `L`, `CX` e afins. Texto livre curto.

**Ativo** (`active`):
Estado de um serviço que pode ser oferecido em novos orçamentos. Um serviço inativo permanece no
catálogo para histórico.
_Avoid_: habilitado, visível

### Fronteiras

**Saldo de estoque**:
Quantidade disponível de uma peça. **Não pertence ao Billing**: é do Execution, assim como reserva,
mínimo de estoque e alerta de estoque baixo. O Billing conhece a peça e o seu preço, nunca quantos
há dela.
_Avoid_: qualquer campo de quantidade em `Part`

**Snapshot de preço**:
Cópia do preço do catálogo feita no instante em que um orçamento é emitido, para que uma alteração
posterior no catálogo não altere orçamentos já emitidos. É um conceito do orçamento, não do catálogo;
está aqui só para deixar claro que **alterar preço no catálogo não tem efeito retroativo**.
