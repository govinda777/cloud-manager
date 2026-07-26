# ADR-010: Gerenciamento do Ciclo de Vida da Conta - Detecção de Drift e Descomissionamento Seguro (Offboarding)

- **Status:** Proposto
- **Data:** 2026-07-26
- **Pilar CloudOps:** Gerenciamento do Ciclo de Vida da Conta (Onboarding, Drift Detection e Offboarding)
- **Relacionado a:** ADR-002, ADR-005, ADR-006

## Contexto e Problema
As contas Cloud criadas pelo `cloud-manager` (CAPE) passam por um processo estruturado de criação e baseline, tornando-se `ACTIVE`. No entanto, o ciclo de vida dessas contas não termina no provisionamento inicial. Ao longo do tempo, dois problemas críticos surgem:
1. **Drift de Configuração:** Administradores alteram manualmente recursos provisionados pelo baseline via console (ex: desligando trilhas de auditoria, abrindo portas de rede de forma insegura), quebrando a conformidade da infraestrutura estabelecida pelo baseline Terraform (ADR-005) de forma silenciosa.
2. **Contas Inativas/Descomissionamento:** Projetos de software chegam ao fim, e as contas correspondentes permanecem ativas na AWS/GCP gerando custos recorrentes e riscos de segurança desnecessários (contas zumbis). É necessário possuir um fluxo estruturado e seguro de encerramento de atividades e destruição lógica dessas contas.

## Comparação com ADRs Existentes
A ADR-002 define a máquina de estados do provisionamento inicial (`CREATED` -> `IN_PROVISIONING` -> `BILLING_LINKED` -> `ACTIVE`). No entanto, a máquina de estados está incompleta se não contemplar as transições de fim de vida (offboarding de contas) e as validações de integridade contra desvios manuais (Drift). Esta ADR expande a máquina de estados e define a lógica de auditoria contínua de infraestrutura e descomissionamento seguro.

## Opções Consideradas

### Opção A: Exclusão Manual das Contas por Operadores de Infraestrutura (Abordagem Humana)
- **Descrição:** Quando uma conta não é mais necessária, o dono abre um chamado e um operador de CloudOps manualmente apaga os recursos e encerra a conta no console dos provedores. A detecção de drift também depende de varreduras humanas ocasionais ou relatórios esporádicos.
- **Prós:**
  - Menor complexidade de automação de destruição no backend do CAPE.
  - Controle e validação humana visual antes de deletar qualquer dado.
- **Contras:**
  - Extremamente lento, ineficiente e propenso a erros (recursos residuais caros podem ficar esquecidos na conta filha desativada).
  - Incapacidade de reagir em tempo hábil a desvios críticos de configuração (drifts), abrindo vulnerabilidades de postura por dias ou semanas.

### Opção B: Extensão da Máquina de Estados (Estados SUSPENDED/TERMINATING) com Execução Contínua de Drift-Detection via IaC
- **Descrição:**
  1. **Detecção Contínua de Drift:** O `cloud-manager` agenda execuções periódicas do tipo `terraform plan` (no modo read-only/audit) contra o baseline correspondente (ADR-005) de cada conta `ACTIVE`. Se o plano indicar desvio em relação ao baseline configurado, a conta é marcada com flag de `DRIFTED` e alertas são enviados ao time responsável, permitindo re-aplicação automática do Terraform para reverter as alterações manuais (reconciliação de estado).
  2. **Ciclo de Vida de Encerramento (Offboarding):** Adicionar novos estados na máquina de estados da conta: `SUSPENDED` (recursos bloqueados temporariamente) e `TERMINATING` (destruição automatizada de dados e exclusão/arquivamento físico da conta filha via API Organizations CloseAccount ou GCP DeleteProject).
- **Prós:**
  - Governança total e automatizada de ponta a ponta (Full Lifecycle Automation).
  - Garantia de que a infraestrutura real reflete fielmente o código de infraestrutura como código (IaC), blindando a conta contra modificações manuais indevidas.
  - Eliminação de custos ocultos com contas inativas ou esquecidas.
- **Contras:**
  - A automação de destruição de contas é crítica e perigosa (risco de deletar dados produtivos valiosos por erro de parâmetro).
  - Exige rotinas severas de auditoria pré-destruição e aprovação de múltiplos gestores (MFA de aprovação).

## Decisão Escolhida
Aprovamos a **Opção B: Extensão da Máquina de Estados (Estados SUSPENDED/TERMINATING) com Execução Contínua de Drift-Detection via IaC**.
A máquina de estados lógica do `cloud-manager` passará a aceitar as seguintes transições adicionais:
- `ACTIVE` $\rightarrow$ `SUSPENDED`: Desativação temporária que suspende direitos de escrita, congela faturamento de serviços auxiliares, mas preserva os dados de forma recuperável.
- `SUSPENDED` $\rightarrow$ `TERMINATING`: Processo destrutivo que dispara pipelines automáticas para apagar os recursos de dados locais (usando ferramentas de limpeza como `aws-nuke` ou rotinas customizadas do baseline) e encerra fisicamente o projeto/conta usando APIs nativas (`Organizations.CloseAccount` / `ResourceManager.Projects.Delete`). Uma conta nesse estado não pode retornar e é marcada como `TERMINATED` ao fim.
Adicionalmente, um cron executor interno no CAPE disparará tarefas semanais de verificação de drift via motor de IaC (ADR-005) para todas as contas em estado `ACTIVE`, reportando e alertando imediatamente desvios no Dashboard unificado (ADR-004).

## Consequências
- **Positivas:**
  - Redução drástica da superfície de ataque corporativa eliminando contas inativas.
  - Garantia de imutabilidade e integridade das políticas de segurança base.
  - Economia financeira substancial por meio de desalocação automática de recursos ociosos desnecessários.
- **Negativas/Riscos:**
  - Risco catastrófico de destruição acidental de contas legítimas devido a bugs de lógica ou comandos indevidos.
- **Plano de Mitigação:**
  - Exigir aprovação manual em "duas mãos" (Quorum de Aprovação por pelo menos 2 Administradores de Cloud de áreas distintas) no Dashboard antes que qualquer conta avance para o estado `TERMINATING`.
  - Contas marcadas para destruição devem permanecer em estado de quarentena/retenção (`SUSPENDED`) por no mínimo 30 dias antes do processamento definitivo das rotinas de destruição física.
