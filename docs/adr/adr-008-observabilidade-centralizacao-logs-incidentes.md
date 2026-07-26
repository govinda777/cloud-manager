# ADR-008: Arquitetura de Observabilidade, Roteamento Central de Logs e Trilha de Auditoria

- **Status:** Proposto
- **Data:** 2026-07-26
- **Pilar CloudOps:** Observabilidade, Centralização de Logs e Resposta a Incidentes
- **Relacionado a:** ADR-001, ADR-005

## Contexto e Problema
Com o crescimento acelerado de contas e projetos sendo provisionados de forma autônoma pelo `cloud-manager` (CAPE), torna-se imperativo possuir uma estratégia unificada de observabilidade e centralização de logs. A ausência de logs centralizados impede a depuração eficiente de falhas em microsserviços do próprio orquestrador, dificulta a identificação de incidentes de segurança (ex: quem realizou uma ação suspeita em qual conta filha) e viola exigências de conformidade regulatória (como a LGPD e PCI-DSS), que demandam trilhas de auditoria imutáveis.

## Comparação com ADRs Existentes
Embora a ADR-001 defina a pureza do domínio em Java, a ADR-002 utilize fila SQS e a ADR-004 exija um painel de gerenciamento no Dashboard, o ecossistema carece de diretrizes específicas sobre o ciclo de vida e centralização de logs e telemetria gerados tanto pelo orquestrador Java quanto pela infraestrutura de baselines e eventos de nuvem. Esta ADR cobre essas necessidades de observabilidade e auditoria.

## Opções Consideradas

### Opção A: Armazenamento e Consulta de Logs Locais nos Provedores de Origem (Abordagem Descentralizada)
- **Descrição:** Manter os logs de aplicação do `cloud-manager` no container/cluster de deploy e instruir os times a consultarem logs de infraestrutura diretamente na console de cada provedor (AWS CloudTrail na AWS e GCP Cloud Logging no GCP) de forma isolada.
- **Prós:**
  - Zero custo de armazenamento centralizado.
  - Simplicidade inicial, pois não requer infraestrutura extra de agregação de logs.
- **Contras:**
  - Ineficiente para depuração rápida e impossibilita correlação de eventos multicloud.
  - Violações de segurança e exclusões intencionais de logs por administradores mal-intencionados nas contas filhas não poderiam ser detectadas a tempo.
  - Dificulta drasticamente auditorias centralizadas de segurança.

### Opção B: Centralização via OpenTelemetry para Aplicações e Roteamento de Logs de Nuvem para um Data Lake de Segurança (SIEM/Audit Bucket)
- **Descrição:** Adotar um padrão em duas frentes de observabilidade:
  1. **Telemetria e Aplicação (cloud-manager):** Instrumentar o backend Java do CAPE de forma agnóstica de fornecedor usando a API do OpenTelemetry (tráfego de logs, métricas e traces estruturados), exportando dados para ferramentas APM (como Jaeger, Datadog ou Prometheus/Grafana).
  2. **Logs de Infraestrutura e Auditoria (CloudTrail e GCP Audit):** No baseline das contas filhas (ADR-005), configurar rotinas automatizadas para exportar logs de atividades (CloudTrail na AWS, Audit Logs no GCP) para tópicos de mensageria locais e roteá-los de volta para um bucket centralizado do time de segurança em uma conta dedicada (Audit/Log Account), que serve como fonte imutável e integrada a uma ferramenta de SIEM.
- **Prós:**
  - Conformidade estrita com padrões regulatórios de segurança e auditoria (WORM - Write Once, Read Many).
  - Agnóstico de vendor no backend Java devido ao uso de OpenTelemetry, respeitando o princípio de isolamento da arquitetura hexagonal.
  - Correlação unificada de traces e logs para depuração de problemas de provisionamento assíncrono.
- **Contras:**
  - Custos adicionais de transferência de dados e armazenamento a longo prazo para logs brutos de auditoria.
  - Maior complexidade para configurar o roteamento seguro e seguro inter-contas de logs de auditoria.

## Decisão Escolhida
Aprovamos a **Opção B: Centralização via OpenTelemetry para Aplicações e Roteamento de Logs de Nuvem para um Data Lake de Segurança (SIEM/Audit Bucket)**.
A aplicação Java do `cloud-manager` utilizará o agente do OpenTelemetry de forma desacoplada do código de domínio (via injeção de dependências e configuração externa de runtime), enviando traces e logs estruturados em JSON para o coletor corporativo.
As contas filhas nascerão com o AWS CloudTrail / GCP Audit Logs habilitados via baseline Terraform (ADR-005) e configurados para publicar em buckets centralizados S3/GCS localizados na conta centralizadora de auditoria (`security-audit-account`). O acesso a esses buckets será altamente restritivo e os logs serão guardados por no mínimo 1 ano em formato imutável (habilitando Object Lock).

## Consequências
- **Positivas:**
  - Rastreabilidade total de todas as ações executadas tanto no `cloud-manager` quanto diretamente nos consoles das nuvens públicas.
  - Facilidade de diagnóstico e redução drástica do tempo médio de reparo (MTTR) de bugs de orquestração.
  - Compliance imediata para auditorias de conformidade regulatória.
- **Negativas/Riscos:**
  - Aumento nos custos de computação e armazenamento devido ao volume expressivo de logs gerados pelas atividades corporativas diárias nas nuvens.
- **Plano de Mitigação:**
  - Aplicar políticas rígidas de ciclo de vida de armazenamento (Lifecycle Policies) nos buckets centrais de logs, movendo-os de classes de armazenamento quente para classes frias e ultra-frias (como S3 Glacier Flexible Retrieval / GCP Archive) após 30 dias de criação.
  - Implementar filtros e regras de exclusão no baseline para logs não-críticos de auditoria de desenvolvimento, mantendo apenas informações cruciais de segurança.
