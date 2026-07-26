# ADR-007: Gestão Financeira, Alertas Orçamentários e Ações de Contenção de Custos (FinOps)

- **Status:** Proposto
- **Data:** 2026-07-26
- **Pilar CloudOps:** Gestão Financeira e Otimização de Custos (FinOps)
- **Relacionado a:** ADR-003, ADR-004, ADR-005

## Contexto e Problema
O faturamento centralizado de contas filhas por meio de contas Seed (conforme decidido na ADR-003) resolve a dependência de billing, mas expõe a organização ao risco de "explosão" de custos não planejados. Para mitigar esse risco de forma ágil, as ações de controle orçamentário e aplicação de limites financeiros devem ser integradas ao ecossistema respeitando o design de separação de responsabilidades (ADR-005).

## Comparação com ADRs Existentes
A ADR-003 garante o vínculo fiscal automático e a ADR-004 estabelece a exigência de um painel visual de FinOps com alertas e rateio de custos. No entanto, o sistema precisa de uma decisão de arquitetura sobre onde e como as restrições e orçamentos físicos de cada conta são aplicados, garantindo isolamento de responsabilidade em relação ao core lógico do CAPE.

## Opções Consideradas

### Opção A: Execução e Controle de Scripts de Desligamento de Recursos pelo Core do CAPE
- **Descrição:** O CAPE conteria rotinas internas que se autenticam em cada conta filha ativada e varrem instâncias ativas para deletá-las ou desligá-las caso o orçamento estoure.
- **Prós:**
  - Lógica de contenção de custos implementada no próprio backend Java.
- **Contras:**
  - Alto acoplamento técnico de APIs de nuvem destrutivas no CAPE.
  - Mistura regras de governança global com regras específicas de infraestrutura de recursos de nuvem, violando a ADR-005.

### Opção B: Provisionamento de Budgets no Repositório IaC da Conta e Contenção via Pipelines e Mecanismos Seed
- **Descrição:** O `cloud-manager` (CAPE) registra os limites de orçamento no banco de dados corporativo e passa essas definições como variáveis para o repositório de IaC da conta (ADR-005).
  1. A pipeline do repositório da conta (via **IAC engine**) provisiona nativamente as estruturas de controle de faturamento (AWS Budgets / GCP Budgets) integradas ao ecossistema de infraestrutura.
  2. Quando ocorrem estouros orçamentários graves (100% ou 120%), webhooks notificam o CAPE.
  3. O CAPE aciona políticas de contenção, alterando os direitos lógicos da conta (com restrições de permissões ou alterando os parâmetros de limite que são aplicados via pipeline de IaC e processados pelo **IAC engine**).
- **Prós:**
  - Isolamento completo: as definições físicas de budgets e regras locais de desligamento de recursos são declaradas como infraestrutura como código no repositório da conta.
  - Segurança aprimorada, sem necessidade de o CAPE reter permissões diretas de escrita e deleção de recursos de computação das contas filhas.
- **Contras:**
  - Exige integração precisa de webhooks entre as notificações das nuvens de faturamento e o CAPE.

## Decisão Escolhida
Aprovamos a **Opção B: Provisionamento de Budgets no Repositório IaC da Conta e Contenção via Pipelines e Mecanismos Seed**.
A API de criação de contas do `cloud-manager` receberá o limite orçamentário mensal (`monthlyBudgetLimit`) e o tipo de ambiente. No momento de instanciar o repositório Git de IaC da conta filha (conforme ADR-005), o CAPE adicionará o arquivo de variáveis do budget ao repositório.
A pipeline do repositório, ao ser executada pelo **IAC engine**, criará as regras físicas de faturamento (AWS Budgets ou GCP Budgets) vinculadas à conta filha correspondente. Ao atingir o orçamento definido (ex: 120% em ambientes de teste), o webhook notifica o CAPE. O CAPE, de maneira segura, atualiza a configuração orçamentária do repositório ou altera as políticas gerais de IAM/SCP (via pipeline), bloqueando novas criações de recursos.

## Consequências
- **Positivas:**
  - Alinhamento rigoroso com as melhores práticas de FinOps e GitOps.
  - O CAPE não precisa possuir credenciais destrutivas de recursos de infraestrutura das contas filhas, delegando as mudanças ao repositório de IaC da conta executado de forma auditável pelo IAC engine.
- **Negativas/Riscos:**
  - Necessidade de garantir que os arquivos de templates de IaC no repositório tratem corretamente os Budgets nativos de cada nuvem.
- **Plano de Mitigação:**
  - O template de repositório de IaC de contas filhas conterá módulos altamente validados e testados pelo time de plataforma para criação de budgets em AWS e GCP, prevenindo erros na pipeline.
