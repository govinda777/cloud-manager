# ADR-007: Gestão Financeira, Alertas Orçamentários e Ações de Contenção de Custos (FinOps)

- **Status:** Proposto
- **Data:** 2026-07-26
- **Pilar CloudOps:** Gestão Financeira e Otimização de Custos (FinOps)
- **Relacionado a:** ADR-003, ADR-004

## Contexto e Problema
O faturamento centralizado de contas filhas por meio de contas Seed (conforme decidido na ADR-003) resolve a dependência de billing, mas expõe a organização ao risco de "explosão" de custos não planejados (ex: uma conta filha que, por erro, provisiona centenas de instâncias caras de banco de dados ou GPU). Sem um controle orçamentário rígido e reações automatizadas integradas ao ciclo de vida da conta no `cloud-manager` (CAPE), falhas de provisionamento e loopings de testes podem estourar orçamentos departamentais inteiros rapidamente.

## Comparação com ADRs Existentes
A ADR-003 garante o vínculo fiscal automático e a ADR-004 estabelece a exigência de um painel visual de FinOps com alertas e rateio de custos. No entanto, o sistema precisa de uma decisão de arquitetura sobre os limites de controle e os mecanismos de reação e governança ativa em caso de estouro orçamentário, indo além de relatórios puramente visuais. Esta ADR aborda essa arquitetura de controle ativo de custos.

## Opções Consideradas

### Opção A: Monitoramento e Envio de Notificações por E-mail / Slack (Puramente Reativo)
- **Descrição:** Usar alertas do AWS Budgets / GCP Budgets para notificar os donos da conta ou administradores do sistema via e-mail, Webhook ou Slack quando os custos acumulados ultrapassarem os percentuais definidos (ex: 80% e 100% do orçamento).
- **Prós:**
  - Baixa complexidade de implementação.
  - Baixo risco de impacto nas operações de produção (nenhum recurso é deletado ou bloqueado sem intervenção humana).
- **Contras:**
  - Depende de ação humana para mitigar os gastos. Em finais de semana ou feriados, os custos podem continuar subindo exponencialmente antes que uma pessoa tome uma atitude de contenção.

### Opção B: Políticas de Orçamento Ativas com Gatilhos de Contenção Automatizados (Políticas Multi-Nível)
- **Descrição:** Definir e provisionar regras orçamentárias com limites de contenção multi-nível (Orçamento Hard e Soft) associados a cada conta filha:
  1. **Nível 1 (Alerta de Gastos):** Ao atingir 80% do orçamento do mês, dispara notificações push, e-mail e Slack para o time responsável.
  2. **Nível 2 (Bloqueio Preventivo - Soft Limit):** Ao atingir 100% do orçamento, remove permissões de criação de novos recursos na conta filha (através de uma SCP restritiva no AWS Organizations ou políticas de IAM modificadas no GCP), congelando a infraestrutura atual, mas sem desligar o que já está rodando.
  3. **Nível 3 (Contenção Severa - Hard Limit - apenas para ambientes não-produtivos):** Ao atingir 120% do orçamento em contas de teste, desliga automaticamente recursos ociosos (como instâncias EC2, RDS e VMs GCP) e restringe totalmente as credenciais de alteração.
- **Prós:**
  - Proteção financeira corporativa absoluta contra gastos descontrolados e inesperados.
  - Redução de custos em ambientes de desenvolvimento e homologação de forma automatizada (sem depender de esforço humano imediato).
- **Contras:**
  - Risco de impacto em ambientes produtivos se os orçamentos não forem bem planejados (por isso a Contenção Severa - Hard Limit só se aplica a contas de ambientes não-produtivos).
  - Exige manutenção fina dos orçamentos cadastrados na base do `cloud-manager`.

## Decisão Escolhida
Aprovamos a **Opção B: Políticas de Orçamento Ativas com Gatilhos de Contenção Automatizados (Políticas Multi-Nível)**.
A API de criação de contas exigirá o limite orçamentário mensal (`monthlyBudgetLimit`) e o tipo de ambiente (`production` ou `non-production`). O baseline da conta (ADR-005) criará instâncias de controle de gastos (AWS Budgets / GCP Budgets) vinculadas a webhooks geridos pelo `cloud-manager`.
Ao receber uma notificação de estouro (100% ou 120%), o `cloud-manager` atuará via suas portas de infraestrutura para aplicar políticas de restrição de direitos de escrita na conta filha correspondente (através de SCP do AWS Organizations ou IAM Policy GCP), suspendendo novas criações de recursos. Para ambientes `non-production` no nível Hard (120%), o CAPE chamará rotinas de automação (via scripts de baseline ou AWS Systems Manager Run Command) para pausar recursos de computação ativos.

## Consequências
- **Positivas:**
  - Eliminação do risco de faturamentos abusivos por falhas operacionais ou ataques cibernéticos (ex: sequestro de instâncias para mineração de criptomoedas).
  - Alinhamento rigoroso com as melhores práticas de governança FinOps.
  - Autonomia dos times com responsabilidade financeira direta.
- **Negativas/Riscos:**
  - Potencial de falsos positivos que impeçam o time de escalar um recurso necessário no meio de um pico de uso (por exemplo, na Black Friday).
- **Plano de Mitigação:**
  - Ambientes categorizados como `production` nunca sofrerão ações de Contenção Severa (Hard Limit - desligamento de recursos) nem de Bloqueio Preventivo sem aprovação expressa de um Diretor/SVP na plataforma.
  - Disponibilizar um botão "Snooze/Emergency Buffer" no Dashboard do `cloud-manager` para administradores autorizados estenderem temporariamente o orçamento de uma conta de forma emergencial por mais 24-48 horas.
