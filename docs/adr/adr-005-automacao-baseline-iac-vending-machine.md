# ADR-005: Orquestração de Baseline de Infraestrutura via Repositório de IaC Templatizado e IAC Engine Externo

- **Status:** Proposto
- **Data:** 2026-07-26
- **Pilar CloudOps:** Automação e Provisionamento de Infraestrutura (IaC e Vending Machine)
- **Relacionado a:** ADR-002, ADR-003

## Contexto e Problema
Após o provisionamento básico de uma conta Cloud (AWS Account ou GCP Project) e a devida vinculação de faturamento (Billing), a conta recém-criada precisa receber o baseline de infraestrutura de rede, segurança e políticas para ser considerada `ACTIVE` com segurança. No entanto, o `cloud-manager` (CAPE) deve seguir o princípio de responsabilidade única (Single Responsibility Principle) e desacoplamento operacional. O CAPE não deve ser responsável por gerenciar diretamente a execução de planos físicos de infraestrutura como código (IaC), nem conter lógicas complexas de compiladores ou runners do Terraform em seu próprio microsserviço.

## Comparação com ADRs Existentes
Embora a ADR-002 defina a máquina de estados baseada em eventos e a ADR-003 exija a vinculação à conta Seed, havia a necessidade de delimitar com exatidão onde se inicia e onde termina o papel do `cloud-manager` no ecossistema de infraestrutura. Esta ADR define a separação estrita de responsabilidades entre o `cloud-manager`, um repositório git templatizado de IaC individual de cada conta, e um projeto externo especializado chamado **IAC engine**.

## Opções Consideradas

### Opção A: Provisionamento Direto e Execução de Terraform de Dentro do CAPE
- **Descrição:** O `cloud-manager` seria responsável por rodar o executável do Terraform/OpenTofu localmente ou via bibliotecas Java, gerenciando os arquivos de estado `.tfstate` diretamente em seu banco de dados ou buckets centrais.
- **Prós:**
  - Todo o fluxo de provisionamento está em uma única ferramenta central.
- **Contras:**
  - Forte acoplamento técnico: qualquer erro de execução de IaC ou gargalo de concorrência afetaria a estabilidade do orquestrador Java principal.
  - Sobrecarga de responsabilidades no core de domínio do CAPE.
  - Dificulta a auditoria e alteração de códigos Terraform de forma independente pelas equipes de Cloud/Plataforma.

### Opção B: Separação de Responsabilidades com Repositórios Git de IaC Dedicados e IAC Engine Independente
- **Descrição:** Dividir o fluxo de trabalho de forma clara e especializada:
  1. **Responsabilidade do `cloud-manager` (CAPE):** Criar a conta Cloud física no provedor, instalar os mecanismos Seed de faturamento/governança, solicitar as faixas de rede (IPAM) e **criar um novo repositório Git dedicado para o código IaC daquela conta específica**, instanciado a partir de um template corporativo padronizado.
  2. **Responsabilidade do IAC Engine:** Um microsserviço especializado e independente que processa execuções de Terraform/OpenTofu.
  3. **Responsabilidade da Pipeline do Repositório de IaC:** Toda a gestão de ciclo de vida, testes, validação de políticas (Lint/OPA) e aplicação física dos recursos do baseline da conta é acionada via esteira de CI/CD integrada à pipeline do próprio repositório Git recém-criado, que delega o processamento pesado do Terraform ao **IAC engine**.
- **Prós:**
  - **Separação Limpa de Responsabilidades:** O CAPE gerencia o ciclo lógico de vida da conta (onboarding, offboarding e metadados), enquanto o repositório IaC templatizado gerencia a declaração física dos recursos da conta, e o IAC engine executa a infraestrutura.
  - **GitOps Nativo:** Mudanças futuras na infraestrutura da conta filha são feitas via Pull Requests no repositório IaC correspondente, gerando histórico e auditoria nativos por Git.
  - **Desacoplamento de Runtime:** O `cloud-manager` não precisa lidar com estados físicos de infraestrutura (como bloqueios de state do Terraform) em tempo de execução de negócio.
- **Contras:**
  - Requer o provisionamento automatizado de repositórios Git (via APIs do GitHub, GitLab ou similar) durante a criação da conta.

## Decisão Escolhida
Aprovamos a **Opção B: Separação de Responsabilidades com Repositórios Git de IaC Dedicados e IAC Engine Independente**.
No estado `IN_PROVISIONING`, o `cloud-manager` realizará as chamadas de API necessárias para instanciar a conta e conectar os mecanismos Seed. Em seguida, chamará a API da ferramenta de Git corporativa para **gerar um novo repositório Git exclusivo para aquela conta**, clonando as pastas de código de baseline a partir de um template homologado.
A partir daí, a pipeline de CI/CD deste repositório assume o controle: ela dispara o processamento dos recursos utilizando o microsserviço externo **IAC engine**. Ao final do deploy com sucesso, a pipeline notifica o `cloud-manager` via webhook, permitindo que o estado da conta avance para `BILLING_LINKED` e finalmente `ACTIVE`.

## Consequências
- **Positivas:**
  - Arquitetura extremamente escalável, resiliente e alinhada às práticas modernas de GitOps e Platform Engineering.
  - Alterações e atualizações na infraestrutura da conta filha podem ser executadas pelas equipes via PRs simples no repositório de IaC da conta, sem necessidade de tocar ou redeployar o `cloud-manager`.
  - O core do `cloud-manager` mantém-se focado estritamente na lógica pura de domínio e governança de contas Cloud.
- **Negativas/Riscos:**
  - Dependência de APIs de provedores de Git para a criação dinâmica de repositórios e configuração de Webhooks de sincronização de estado.
- **Plano de Mitigação:**
  - Implementar mecanismos de Retry robustos na integração com APIs de Git (como GitHub/GitLab).
  - Centralizar os templates de repositório de IaC de modo que qualquer alteração de baseline possa ser propagada via Pull Requests em lote automáticos para os repositórios das contas filhas.
