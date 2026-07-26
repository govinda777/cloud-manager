# ADR-008: Arquitetura de Observabilidade, Roteamento Central de Logs e Trilha de Auditoria

- **Status:** Proposto
- **Data:** 2026-07-26
- **Pilar CloudOps:** Observabilidade, Centralização de Logs e Resposta a Incidentes
- **Relacionado a:** ADR-001, ADR-005

## Contexto e Problema
Com o crescimento acelerado de contas e projetos sendo provisionados de forma autônoma pelo `cloud-manager` (CAPE), torna-se imperativo possuir uma estratégia unificada de observabilidade e centralização de logs. Seguindo o padrão de separação de responsabilidades (ADR-005), os logs gerados pelas ferramentas de infraestrutura física de cada conta e pelo orquestrador Java devem ser coletados e tratados de forma independente, auditável e imutável.

## Comparação com ADRs Existentes
Embora a ADR-001 defina a pureza do domínio em Java, a ADR-002 utilize fila SQS e a ADR-004 exija um painel de gerenciamento no Dashboard, o ecossistema carece de diretrizes específicas sobre onde e como as definições de infraestrutura de roteamento de logs de auditoria das contas filhas são provisionadas e estruturadas. Esta ADR aborda essa arquitetura.

## Opções Consideradas

### Opção A: Provisionamento Direto de Buckets de Log pelo CAPE
- **Descrição:** O CAPE criaria ativamente os recursos e buckets de logs nas contas filhas utilizando SDKs do provedor de nuvem em Java, gerenciando as conexões em tempo de execução.
- **Prós:**
  - Configuração direta e controlada de forma centralizada pelo backend do CAPE.
- **Contras:**
  - Violaria a separação de responsabilidades definida na ADR-005, inflando o CAPE com lógicas de infraestrutura complexas específicas de cada provedor de nuvem.

### Opção B: Provisionamento de Roteamento de Logs via Template de Repositório IaC e IAC Engine
- **Descrição:** Toda a infraestrutura física de auditoria e telemetria local da conta filha é declarada no template de seu próprio repositório de IaC (ADR-005) e aplicada pelo **IAC engine**:
  1. O template de IaC da conta filha declara nativamente a ativação de ferramentas de trilha de auditoria (AWS CloudTrail / GCP Audit Logs) e configura os destinos para os buckets centrais de auditoria segura corporativa.
  2. O **IAC engine** aplica essas configurações na pipeline do repositório da conta filha.
  3. A telemetria da aplicação principal do `cloud-manager` (CAPE) é enviada de forma independente via OpenTelemetry para APMs agnósticos.
- **Prós:**
  - Estrita aderência ao modelo de responsabilidades (ADR-005): o CAPE não gerencia recursos físicos de infraestrutura de log, apenas cria o repositório da conta a partir do template pré-configurado.
  - O código do baseline de logs pode ser atualizado de forma independente do `cloud-manager` no repositório de templates corporativos.
- **Contras:**
  - Qualquer desvio ou falha de provisionamento da infraestrutura de logs deve ser tratado e reportado pela esteira de CI/CD do repositório de IaC.

## Decisão Escolhida
Aprovamos a **Opção B: Provisionamento de Roteamento de Logs via Template de Repositório IaC e IAC Engine**.
A infraestrutura de captação e envio de logs de auditoria (AWS CloudTrail / GCP Audit Logs) de cada conta filha será obrigatoriamente incluída no template de repositório de IaC padrão. No momento em que a pipeline do repositório de IaC da conta filha é executada pelo **IAC engine**, esses recursos de segurança são criados e travados na nuvem correspondente.
Os logs de auditoria e segurança serão roteados para buckets corporativos imutáveis e protegidos na conta de auditoria central (`security-audit-account`). A telemetria da aplicação do `cloud-manager` (CAPE) será coletada via instrumentação externa do OpenTelemetry e enviada para coletores centrais.

## Consequências
- **Positivas:**
  - Separação completa de responsabilidades no ciclo de observabilidade.
  - Facilidade de manutenção de baselines de segurança de logs via GitOps no template do repositório de IaC.
- **Negativas/Riscos:**
  - Mudanças nas especificações técnicas de logs de auditoria exigem atualização do repositório de templates IaC corporativo.
- **Plano de Mitigação:**
  - Utilizar versionamento semântico para os templates de repositórios de IaC, permitindo que as contas filhas atualizem seus baselines de logs via branches e PRs automáticos.
