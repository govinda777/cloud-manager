# ADR-006: Implementação de Guardrails de Segurança e Políticas como Código (SecOps & Compliance)

- **Status:** Proposto
- **Data:** 2026-07-26
- **Pilar CloudOps:** Governança, Segurança e Compliance (SecOps & Guardrails)
- **Relacionado a:** ADR-004, ADR-005

## Contexto e Problema
O provisionamento descentralizado e automático de contas Cloud gera riscos severos de conformidade, segurança e vazamento de dados. Usuários e desenvolvedores com acesso administrativo às contas filhas podem criar recursos expostos à internet pública ou em desconformidade com os padrões. O ecossistema de governança precisa garantir de forma preventiva e reativa que todas as contas permaneçam seguras, seguindo o modelo de separação de responsabilidades (conforme ADR-005).

## Comparação com ADRs Existentes
A ADR-004 aborda a necessidade de uma interface gráfica que reporte vulnerabilidades de postura em tempo real. Esta ADR detalha como os Guardrails preventivos e reativos são estruturados entre as barreiras de desenvolvimento (repositório IaC da conta + IAC engine) e o monitoramento central de postura.

## Opções Consideradas

### Opção A: Validação e Varredura Centralizada pelo CAPE via Polling de APIs de Segurança
- **Descrição:** O `cloud-manager` seria responsável por ler e validar de forma ativa o código de cada repositório e rodar verificações de segurança no seu backend Java.
- **Prós:**
  - Lógica centralizada de validação em um único ponto.
- **Contras:**
  - Desperdício de recursos de computação do core de negócio.
  - Viola a separação de responsabilidades definida na ADR-005: o CAPE deve gerenciar o ciclo de vida e não atuar como motor de validação estática de códigos de infraestrutura de terceiros.

### Opção B: Políticas como Código (OPA/Rego) na Pipeline do Repositório IaC executadas pelo IAC Engine e Guardrails Contínuos de Nuvem
- **Descrição:** Distribuir as políticas e validações de segurança em duas camadas principais:
  1. **Camada Preventiva (Shift-Left na Pipeline do Repositório IaC):** No pipeline do repositório Git de IaC criado para a conta (a partir do template), etapas automatizadas invocam o **IAC engine** para auditar os arquivos de plano de infraestrutura do Terraform contra regras escritas em OPA (Open Policy Agent) ou Checkov, antes de aplicar qualquer recurso.
  2. **Camada Reativa (Monitoramento Contínuo):** Implantar, por meio dos recursos gerados pelo pipeline de IaC, políticas nativas de conformidade (AWS Config Rules com auto-remediação, SCPs no AWS Organizations, e Organization Policies no GCP). O `cloud-manager` atua consumindo eventos de desvios dessas soluções para relatar a postura no Dashboard (ADR-004).
- **Prós:**
  - Bloqueia vulnerabilidades de infraestrutura diretamente na esteira de CI/CD do repositório IaC, antes que o recurso chegue a existir na nuvem física.
  - Totalmente alinhado à separação de responsabilidades da ADR-005: o processamento pesado de validações ocorre no pipeline IaC delegando ao IAC engine.
- **Contras:**
  - Requer que o template do repositório de IaC de contas filhas já venha pré-configurado com as esteiras de teste de OPA/Rego.

## Decisão Escolhida
Aprovamos a **Opção B: Políticas como Código (OPA/Rego) na Pipeline do Repositório IaC executadas pelo IAC Engine e Guardrails Contínuos de Nuvem**.
As políticas preventivas de segurança (Policy-as-Code) serão integradas nativamente como etapas obrigatórias na esteira de CI/CD de cada repositório de IaC templatizado. O pipeline fará o upload ou chamará a validação do plano estático do Terraform no **IAC engine**, comparando-o com regras corporativas centralizadas escritas em OPA/Rego. Qualquer infraestrutura fora do padrão (ex: IPs públicos abertos) causa o bloqueio imediato do pipeline, impedindo que o deploy seja consolidado.
As regras de compliance de runtime (AWS Config e GCP Security Health Analytics) instaladas como recursos de baseline via pipeline de IaC garantirão a segurança contínua, reportando incidentes que serão recebidos assincronamente pelo CAPE para atualização do painel de segurança.

## Consequências
- **Positivas:**
  - Mitigação precoce de riscos de segurança no início do ciclo de vida dos recursos (Shift-Left).
  - Isolamento de execução de validações estáticas de infraestrutura nas pipelines dos repositórios e no IAC engine, sem consumo de processamento no CAPE.
- **Negativas/Riscos:**
  - Necessidade de gerenciar e atualizar os scripts Rego de política em todas as esteiras de repositório.
- **Plano de Mitigação:**
  - Manter as políticas de OPA/Rego em um repositório Git centralizado de governança. A pipeline de CI/CD do repositório IaC de cada conta filha clonará/puxará essas regras centralizadas dinamicamente em tempo de execução para garantir que todos usem sempre as regras de segurança mais recentes.
