# ADR-007: Gestão Financeira, Alertas Orçamentários e Contenção Baseada em Controles Semente Preexistentes (FinOps)

- **Status:** Proposto
- **Data:** 2026-07-26
- **Pilar CloudOps:** Gestão Financeira e Otimização de Custos (FinOps)
- **Relacionado a:** ADR-003, ADR-004, ADR-005

## Contexto e Problema
O faturamento centralizado de contas filhas através de uma conta Seed (ADR-003) resolve o vínculo comercial e fiscal, mas expõe a organização ao risco de gastos descontrolados ou loops de criação de recursos em contas de desenvolvimento. O provisionamento e monitoramento individualizado de orçamentos (budgets) via código Terraform na própria conta filha possui sérias limitações:
1. Requer complexidade de deploy de IaC local para criar Budgets específicos para cada nova conta.
2. Pode ser facilmente contornado ou deletado por administradores locais da conta filha via console.
3. Não aproveita o potencial de controle centralizado e consolidado que as contas mãe/Seed já oferecem nativamente nas nuvens (como AWS Organizations e GCP Billing Accounts).

Precisamos de uma arquitetura de FinOps robusta, integrada nativamente ao mecanismo Seed, sem depender de declaração de recursos via Terraform para cada subconta.

## Comparação com ADRs Existentes
A ADR-003 garante o faturamento herdado de contas Seed, e a ADR-004 prevê um painel visual de custos no Dashboard. No entanto, o sistema carecia de definições de arquitetura sobre onde reside o monitoramento ativo e os alertas financeiros. Esta ADR redefine a mecânica: em vez de criar regras locais via Terraform, o controle financeiro utiliza **mecanismos semente preexistentes** estruturados na conta raiz.

## Opções Consideradas

### Opção A: Declaração e Provisionamento de Recursos de Faturamento via Código Terraform Local
- **Descrição:** Cada conta filha declara e provisiona seus próprios Budgets, alarmes CloudWatch e tópicos SNS locais através de código de infraestrutura como código (IaC).
- **Prós:**
  - Descentralização, permitindo que cada time configure limites conforme desejado no repositório.
- **Contras:**
  - Alto risco de exclusão indevida ou alteração manual por usuários com privilégios locais.
  - Excesso de código duplicado e retrabalho para configurar alertas repetitivos em cada repositório de conta filha.
  - Fraca governança: o time financeiro central perde a capacidade de consolidar e centralizar as regras de bloqueio.

### Opção B: Uso de Controles de Faturamento Preexistentes e Centralizados nas Contas Semente (Seed) e AWS Organizations / GCP Billing
- **Descrição:** Utilizar a infraestrutura preexistente e consolidada de faturamento na conta Seed (mãe):
  1. **Regras Globais na Semente:** A conta Seed (mãe) já nasce pré-configurada com grupos de orçamento consolidados (AWS Budgets, GCP Billing Budgets) e detectores centralizados de anomalias de custos (AWS Cost Anomaly Detection / GCP Cost Anomaly).
  2. **Vinculação Automática:** No momento em que o `cloud-manager` (CAPE) cria a conta filha e realiza a associação de billing na conta Seed (conforme ADR-003), ela é automaticamente inserida na hierarquia organizacional ou unidade organizacional (OU) monitorada e herda as políticas preexistentes de controle orçamentário da conta semente.
  3. **Alertas e Notificações:** Alertas de consumo e custos anômalos gerados no nível da conta Seed são publicados em um tópico de mensageria centralizado da conta mãe (ex: SNS/PubSub central) e roteados de volta para o CAPE.
  4. **Contenção Lógica:** Se houver estouro crítico de orçamento, o CAPE aciona políticas simples de restrição através de chamadas de API centralizadas na conta mãe (por exemplo, aplicando uma Service Control Policy - SCP de bloqueio preventivo via AWS Organizations, sem necessidade de tocar ou alterar recursos individuais de computação da conta filha).
- **Prós:**
  - **Inviolabilidade:** Usuários locais da conta filha não possuem permissão para apagar ou adulterar as regras de faturamento, pois os limites residem e são fiscalizados a partir da conta mãe/Semente de forma centralizada.
  - **Simplicidade Técnica:** O pipeline de IaC do repositório da conta filha (ADR-005) fica totalmente isento de configurar recursos de faturamento, agilizando o provisionamento.
  - **Consolidação Nativa:** Facilita a consolidação financeira para auditorias globais de custos.
- **Contras:**
  - Requer que a conta Seed já tenha sido devidamente provisionada e configurada previamente com esses mecanismos de detecção centralizados de faturamento antes de desdobrar contas filhas.

## Decisão Escolhida
Aprovamos a **Opção B: Uso de Controles de Faturamento Preexistentes e Centralizados nas Contas Semente (Seed) e AWS Organizations / GCP Billing**.
O monitoramento e a aplicação de limites orçamentários não serão controlados via código Terraform na conta filha. Toda a infraestrutura física de controle de custos será centralizada e herdada a partir das contas Semente preexistentes.
O `cloud-manager` (CAPE) apenas repassa o limite orçamentário lógico no banco de dados e associa a nova conta filha às tags e políticas corporativas globais de faturamento configuradas na semente. Eventos de desvios e alarmes disparados na conta Seed serão enviados ao CAPE via tópico SNS/PubSub integrado de monitoramento corporativo central.
Caso um estouro de orçamento ocorra, a contenção será aplicada de forma centralizada por meio de regras aplicadas no nível da organização (como uma SCP restritiva anexada na conta filha a partir da conta raiz), garantindo que os dados e recursos físicos de produção permaneçam preservados enquanto a conta é bloqueada de forma limpa e segura contra novas cobranças.

## Consequências
- **Positivas:**
  - Governança de faturamento absolutamente blindada contra adulterações locais.
  - Menor complexidade e tempo de execução na criação das contas filhas, que herdam as estruturas de custo instantaneamente ao serem ligadas à semente.
  - Gerenciamento unificado e simplificado para o time de FinOps corporativo.
- **Negativas/Riscos:**
  - Dependência estrita de que a conta Seed preexistente esteja com a infraestrutura de billing, limites globais e detectores de anomalias corretamente criados e operacionais.
- **Plano de Mitigação:**
  - Desenvolver um validador de integridade (Health Check) no CAPE que verifica a saúde e conectividade com a API de Billing da conta Seed antes de iniciar qualquer fluxo de criação de conta filha.
