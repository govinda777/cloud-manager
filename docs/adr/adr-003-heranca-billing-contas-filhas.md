# ADR 003: Herança de Faturamento para Contas Filhas via Contas Seed

**Status:** APROVADO
**Data:** 2026-07-25
**Autor:** Arquitetura de Software

---

## Contexto
Nenhuma conta filha (AWS Account / GCP Project) pode ser órfã de faturamento. Ela deve herdar o faturamento estruturado de contas centralizadoras previamente configuradas e validadas.

## Decisão
O evento de criação deve exigir o identificador da conta Seed correspondente (`seedAccountId`). A etapa `BILLING_LINKED` fará a associação automática via API da AWS (Organizations AttachPolicy/Billing) e GCP (Cloud Billing API) antes de marcar a conta como `ACTIVE`.

## Consequências

### Positivas
* Garantia de conformidade orçamentária automática.
* Governança centralizada e automatização total do ciclo de vida financeiro.

### Negativas
* Dependência direta da integridade, cotas disponíveis e saúde da conta Seed (se a conta Seed falhar ou atingir limites, novas contas filhas não podem ser ativadas).
