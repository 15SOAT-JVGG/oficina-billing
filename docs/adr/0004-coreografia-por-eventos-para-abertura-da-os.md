# Coreografia por eventos para a abertura da OS

Na integração da issue #109, o `oficina-app` mantém a propriedade da Ordem de Serviço e o Billing a
do orçamento. O Billing recebe `work-order.opened`, resolve as referências no próprio catálogo e
registra o resultado. A comunicação é assíncrona e coreografada, conforme a spec #106: não há
coordenador central nem acesso ao banco do outro serviço. A aplicação usa tópicos SNS FIFO, filas
SQS FIFO, inbox por `eventId` e outbox transacional. Os schemas v1 continuam provisórios até
validação conjunta entre os serviços.

## Considered Options

**Orquestrador síncrono no OS ou consulta direta ao banco Billing.** Rejeitados: criariam acoplamento
temporal ou de persistência entre serviços e não seguem a decisão de coreografia da spec #106.

## Consequences

O HTTP de abertura confirma a criação da OS (`201`); a projeção do orçamento chega depois por evento.
Consumidores devem tolerar duplicatas e retentativas. Resultados incompatíveis com a solicitação
pendente são rejeitados pelo OS. A compatibilidade final do contrato depende da validação do Billing
e do OS.
