# ADR 002: Orquestração Baseada em Eventos com Máquina de Estados

**Status:** APROVADO
**Data:** 2026-07-25
**Autor:** Arquitetura de Software

---

## Contexto
A criação de contas na AWS/GCP é um processo demorado (operações assíncronas de infraestrutura). As chamadas síncronas HTTP são inviáveis devido ao risco de timeout e à necessidade de resiliência.

## Decisão
Adotar transições de estado orientadas a eventos utilizando AWS SQS. As etapas passam de forma estrita por `CREATED` $\rightarrow$ `IN_PROVISIONING` $\rightarrow$ `BILLING_LINKED` $\rightarrow$ `ACTIVE`. Em caso de falha, transita para `FAILED` gravando o `historyLog` de erro.

## Consequências

### Positivas
* Resiliência contra timeouts.
* Desacoplamento e escalabilidade do fluxo de provisionamento.
* Idempotência de execução garantida pelas transições de estado.

### Negativas
* Complexidade no acompanhamento eventual do estado da conta pelo cliente final (polling ou websockets adicionais).
