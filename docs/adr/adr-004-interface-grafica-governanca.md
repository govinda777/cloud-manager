# ADR 004: Interface Gráfica de Governança para Contas Cloud

**Status:** APROVADO
**Data:** 2026-07-25
**Autor:** Arquitetura de Software

---

## Contexto
Toda conta cloud provisionada (AWS/GCP) precisa nascer associada a uma interface gráfica unificada (Dashboard) que permita o gerenciamento, monitoramento e controle de conformidade por parte dos administradores e usuários. É necessário centralizar o controle de segurança, custos e organização das contas de maneira intuitiva.

## Decisão
Disponibilizar uma interface gráfica de governança centralizada contendo as seguintes visões/telas obrigatórias:

1. **Vulnerabilidade da Conta**: Dashboard de postura de segurança em tempo real, integrando feeds de ferramentas como AWS Security Hub, GCP Security Command Center, ou scanners open-source, reportando vulnerabilidades, portas abertas e desvios de compliance.
2. **FinOps (Gestão de Custos)**: Painel financeiro exibindo a evolução dos gastos da conta filha em relação aos limites estabelecidos pela conta Seed, incluindo alertas de anomalias de faturamento, sugestões de otimização de recursos ociosos e rateio de custos.
3. **Tags (Conformidade de Metadados)**: Painel de conformidade de tags para auditoria. Garante que os recursos criados na conta possuam as tags obrigatórias definidas na política global (ex: `env`, `owner`, `project`). Permite a aplicação em lote de tags ausentes.
4. **Gerenciamento Geral de Contas**: Funcionalidades administrativas básicas, incluindo listagem de contas filhas ativas, histórico de transições de estados (`AccountState`) e logs de auditoria de operações.

A interface gráfica será construída como uma SPA (Single Page Application) consumindo as APIs do orquestrador via um padrão BFF (Backend For Frontend), mantendo as regras de negócio puras isoladas no backend.

## Consequências

### Positivas
* **Governança Unificada**: Consolidação de segurança, custos e metadados em um único local, eliminando a necessidade de acessar os consoles nativos dos provedores para tarefas rotineiras de conformidade.
* **Visibilidade Pró-ativa**: Identificação imediata de desvios de segurança ou faturamento assim que a conta é ativada.
* **Facilidade de Auditoria**: Gestão centralizada de tags facilita auditorias e relatórios de custos detalhados.

### Negativas
* **Esforço de Desenvolvimento**: Necessidade de desenvolver e manter uma interface frontend dedicada e um BFF adicional.
* **Custos de Integração**: Taxas de API ou custos adicionais de armazenamento para sincronizar e persistir logs de vulnerabilidade e faturamento.
