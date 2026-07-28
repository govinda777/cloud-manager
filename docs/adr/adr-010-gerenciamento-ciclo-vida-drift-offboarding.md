# ADR-010: Gerenciamento do Ciclo de Vida da Conta - Detecção de Drift e Descomissionamento Seguro (Offboarding)

- **Status:** Proposto
- **Data:** 2026-07-26
- **Pilar CloudOps:** Gerenciamento do Ciclo de Vida da Conta (Onboarding, Drift Detection e Offboarding)
- **Relacionado a:** ADR-002, ADR-005, ADR-006

## Contexto e Problema
As contas Cloud criadas pelo `cloud-manager` (CAPE) passam por um processo estruturado de criação, tornando-se `ACTIVE` após a aplicação bem-sucedida do baseline via pipeline do repositório IaC da conta. No entanto, o ciclo de vida não termina no provisionamento. É necessário monitorar desvios de configuração física em relação ao template de baseline (Drift) e prover um fluxo seguro e automatizado para o encerramento definitivo (Offboarding) das contas filhas, respeitando a separação de responsabilidades (ADR-005).

## Comparação com ADRs Existentes
A ADR-002 define a máquina de estados inicial (`CREATED` -> `IN_PROVISIONING` -> `BILLING_LINKED` -> `ACTIVE`). Esta ADR estende a máquina de estados do CAPE para abranger os fluxos de pós-ativação (auditoria de drift contínuo e destruição/offboarding seguro), especificando como as ações são coordenadas entre o CAPE, os repositórios de IaC de cada conta e o **IAC engine**.

## Opções Consideradas

### Opção A: Detecção de Drift e Destruição de Recursos Executados de Forma Nativa no CAPE
- **Descrição:** O CAPE realizaria varreduras periódicas de rede e de recursos chamando as APIs de nuvem diretamente de seu código Java para identificar desvios. Para a exclusão, o CAPE executaria códigos Java destrutivos na conta filha.
- **Prós:**
  - Lógica centralizada de controle e destruição.
- **Contras:**
  - Alto risco operacional: qualquer bug no core Java do CAPE poderia apagar dados indevidamente em contas produtivas.
  - Mistura de responsabilidades: o CAPE não foi desenhado para calcular planos físicos de diferença de infraestrutura (tarefa do Terraform/OpenTofu) e nem deve reter credenciais diretas de alteração de recursos físicos.

### Opção B: Verificações de Drift e Destruição Coordenadas pelas Pipelines de IaC via IAC Engine
- **Descrição:** Transferir as atividades pesadas de infraestrutura de pós-onboarding para o ecossistema de IaC descentralizado:
  1. **Detecção de Drift:** O `cloud-manager` agenda e aciona rotinas periódicas (via webhook de agendamento ou cron corporativo) que iniciam uma tarefa de auditoria no pipeline do repositório de IaC da conta (ADR-005). Esse pipeline roda um plano de leitura do estado (`terraform plan` read-only) utilizando o **IAC engine**. Em caso de desvio em relação ao baseline do template, o pipeline notifica o CAPE que sinaliza a conta como `DRIFTED` no Dashboard (ADR-004).
  2. **Ciclo de Offboarding:** Adicionar os estados `SUSPENDED` e `TERMINATING` na máquina de estados lógica do CAPE. No estado `TERMINATING`, o CAPE altera o status lógico no repositório da conta filha e aciona a pipeline de destruição do próprio repositório. O **IAC engine** executa a destruição dos recursos físicos em conformidade com o código IaC. Após o sucesso da limpeza física de recursos, o CAPE remove o repositório Git correspondente e encerra fisicamente a conta filha na AWS/GCP (via API Organizations CloseAccount ou GCP DeleteProject).
- **Prós:**
  - **Segurança Absoluta:** O CAPE coordena o fluxo de estados de negócios, enquanto as tarefas de modificação e destruição de infraestrutura rodam no contexto auditável e de menor privilégio do pipeline de IaC executado pelo IAC engine.
  - **Auditabilidade Extrema:** Cada execução de drift e plano de destruição gera logs completos e registros de histórico na esteira de CI/CD do repositório IaC correspondente.
- **Contras:**
  - Requer maior fluxo de transições de status assíncronas entre o CAPE e os pipelines dos repositórios.

## Decisão Escolhida
Aprovamos a **Opção B: Verificações de Drift e Destruição Coordenadas pelas Pipelines de IaC via IAC Engine**.
A máquina de estados lógica do `cloud-manager` passa a englobar os estados de pós-ativação de forma integrada. O monitoramento contínuo de Drift será disparado através de execuções automatizadas e agendadas na esteira de CI/CD do repositório IaC de cada conta filha, que delega o cálculo das diferenças ao **IAC engine**.
Para o processo de descarte de contas (Offboarding), o CAPE moverá a conta para `SUSPENDED` (quarentena lógica de 30 dias com bloqueio de novos deploys). Findada a quarentena, o CAPE transita a conta para `TERMINATING` e invoca a pipeline de destruição (`terraform destroy`) no repositório de IaC correspondente, processado pelo **IAC engine**. Somente após a confirmação do sucesso da deleção de todos os recursos pelo pipeline, o CAPE realiza a deleção do repositório Git e o fechamento administrativo da conta filha.

## Consequências
- **Positivas:**
  - Redução drástica da superfície de ataque operacional.
  - O core lógico do CAPE permanece isolado e seguro de lógicas físicas destrutivas de recursos.
  - Rastreabilidade de modificações manuais (Drifts) integrada ao GitOps.
- **Negativas/Riscos:**
  - O fluxo de exclusão requer múltiplos webhooks e retornos estáveis de pipelines para confirmar a finalização segura de cada etapa.
- **Plano de Mitigação:**
  - Exigir "Quorum de Aprovação" (MFA / aprovação conjunta de dois diretores de Cloud) na UI do `cloud-manager` para habilitar a transição de `SUSPENDED` para `TERMINATING` de qualquer conta filha.
  - Implementar verificação de recursos vazios ("zero resources audit") antes do fechamento administrativo final do provedor de nuvem.
