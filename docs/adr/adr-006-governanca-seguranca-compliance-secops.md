# ADR-006: Delimitação de Escopo de SecOps e Delegação de Guardrails Preventivos para a Engine de IaC

- **Status:** Proposto
- **Data:** 2026-07-26
- **Pilar CloudOps:** Governança, Segurança e Compliance (SecOps & Guardrails)
- **Relacionado a:** ADR-004, ADR-005

## Contexto e Problema
A segurança, conformidade e aplicação de políticas de infraestrutura como código (Policy-as-Code/Guardrails) são críticas para garantir o provisionamento correto de recursos nas contas filhas. Contudo, para manter a pureza arquitetural do `cloud-manager` (CAPE) e obedecer à separação rigorosa de responsabilidades estabelecida na ADR-005, é fundamental delimitar o escopo do que o CAPE valida ativamente. O CAPE não deve atuar como um motor de análise de segurança estática ou varredura de código de infraestrutura, sob o risco de inchar suas regras de negócio e acoplá-lo a lógicas pesadas de verificação de segurança.

## Comparação com ADRs Existentes
A ADR-004 prevê um painel visual (Dashboard) que exibe as vulnerabilidades encontradas nas contas de forma reativa. A presente ADR estabelece a separação clara e a exclusão de escopo do CAPE quanto à validação ativa de segurança preventiva, formalizando que essa tarefa pertence unicamente à pipeline de IaC do repositório de cada conta, executada de forma nativa pela **IAC engine**.

## Opções Consideradas

### Opção A: Validação Ativa de Postura de Código IaC Dentro do Core do CAPE
- **Descrição:** O CAPE receberia ou leria os arquivos Terraform gerados, interpretando-os e aplicando regras de validação estática de segurança preventivamente antes de dar andamento às transições de estado.
- **Prós:**
  - O controle preventivo total estaria sob a governança direta e centralizada do backend em Java.
- **Contras:**
  - Mistura regras de governança lógica de contas com validação estática de recursos físicos.
  - Aumento expressivo da complexidade do microsserviço com a inclusão de motores de análise de IaC.

### Opção B: Exclusão Total de Escopo Preventivo do CAPE e Delegação Exclusiva para a IAC Engine e Pipelines
- **Descrição:** Declarar que o escopo de validação de postura de segurança preventiva e a análise de políticas como código (Policy-as-Code) estão totalmente fora do escopo do `cloud-manager` (CAPE). Toda essa responsabilidade é delegada à pipeline do repositório IaC templatizado de cada conta, cujo processamento e validação física de regras são efetuados exclusivamente pela **IAC engine** durante a aplicação dos recursos.
- **Prós:**
  - Isolamento completo: o CAPE não possui qualquer conhecimento ou código referente a regras preventivas de recursos de segurança locais (como portas abertas ou criptografia de buckets).
  - A **IAC engine** atua como o único motor responsável por validar a segurança e impedir deploys fora de conformidade na nuvem física.
  - Redução drástica da complexidade técnica e tamanho do codebase do `cloud-manager` (CAPE).
- **Contras:**
  - O CAPE dependerá exclusivamente dos alertas gerados de volta pelas ferramentas de runtime e da notificação de sucesso/falha do pipeline para saber se as políticas foram obedecidas.

## Decisão Escolhida
Aprovamos a **Opção B: Exclusão Total de Escopo Preventivo do CAPE e Delegação Exclusiva para a IAC Engine e Pipelines**.
O `cloud-manager` (CAPE) está explicitamente **isento** de validar, ler ou analisar regras preventivas de segurança física e infraestrutura como código.
Toda a responsabilidade de executar auditorias estáticas de código (como testes OPA/Rego, checagem de privilégios ou busca por portas expostas) é delegada à pipeline de CI/CD do repositório IaC da conta filha. A validação de conformidade da infraestrutura física gerada será executada exclusivamente pelo microsserviço especializado **IAC engine** no momento da execução do deploy.
O CAPE apenas consome as notificações de resultado (sucesso ou falha) da pipeline e as vulnerabilidades reportadas de runtime (via monitoramento contínuo dos provedores de nuvem) para exibição visual no Dashboard (ADR-004).

## Consequências
- **Positivas:**
  - Máxima simplicidade e coesão no codebase do `cloud-manager` (CAPE).
  - Alinhamento rigoroso ao princípio de responsabilidade única e separação de responsabilidades.
  - Se novas regras de conformidade de segurança forem adicionadas ou alteradas, não há necessidade de qualquer alteração de código ou implantação no CAPE, já que essa gestão reside integralmente nas regras da **IAC engine**.
- **Negativas/Riscos:**
  - Falhas na configuração do pipeline ou no **IAC engine** podem permitir o provisionamento de recursos fora de conformidade sem o bloqueio precoce do CAPE.
- **Plano de Mitigação:**
  - Manter o template padrão de repositório de IaC (ADR-005) com a integração nativa e inquebrável para a esteira de validação do **IAC engine**, garantindo que nenhuma conta seja provisionada ignorando essas validações.
