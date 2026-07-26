# ADR-006: Implementação de Guardrails de Segurança e Políticas como Código (SecOps & Compliance)

- **Status:** Proposto
- **Data:** 2026-07-26
- **Pilar CloudOps:** Governança, Segurança e Compliance (SecOps & Guardrails)
- **Relacionado a:** ADR-004, ADR-005

## Contexto e Problema
O provisionamento descentralizado e automático de contas Cloud gera riscos severos de conformidade, segurança e vazamento de dados. Usuários e desenvolvedores com acesso administrativo às contas filhas podem, por erro ou descuido, criar recursos expostos à internet pública (ex: buckets S3 abertos, instâncias de banco sem autenticação, portas administrativas SSH/RDP liberadas para o mundo). O `cloud-manager` precisa garantir que as contas nasçam e permaneçam seguras por meio de políticas preventivas e reativas (Guardrails) contínuas, sem interferir na agilidade do provisionamento.

## Comparação com ADRs Existentes
A ADR-004 aborda a necessidade de uma interface gráfica que reporte vulnerabilidades de postura em tempo real, mas não especifica o mecanismo e as decisões arquiteturais de como os Guardrails preventivos e reativos são implementados, estruturados e fiscalizados no nível de nuvem. Esta ADR detalha a engine de política como código (Policy-as-Code) e a arquitetura de controle ativo de segurança.

## Opções Consideradas

### Opção A: Implementação de Verificações Customizadas via Código Backend no CAPE
- **Descrição:** Escrever algoritmos customizados em Java dentro do `cloud-manager` que varrem periodicamente as APIs da AWS/GCP à procura de violações de segurança.
- **Prós:**
  - Todo o controle é centralizado no código-fonte principal do backend Java.
  - Elimina a dependência de produtos externos ou de regras em outras linguagens de política.
- **Contras:**
  - Altíssimo custo de desenvolvimento e manutenção para cobrir centenas de regras de segurança em constante evolução.
  - Ineficiente, lento e com alto consumo de cota de chamadas de API dos provedores de nuvem por causa do polling recorrente.
  - Mistura regras de infraestrutura e postura com regras puras de lógica de domínio corporativo.

### Opção B: Uso Combinado de Policy-as-Code Preventivo (OPA/Rego) e Guardrails Nativos Reativos (AWS Config / GCP Org Policies / SCPs)
- **Descrição:** Utilizar uma abordagem em duas camadas:
  1. **Camada Preventiva:** Avaliar os planos de execução do Terraform/OpenTofu (conforme decidido na ADR-005) contra políticas escritas em OPA/Rego (Open Policy Agent) antes do deploy.
  2. **Camada Reativa/Contínua:** Implantar, por meio do baseline da conta, regras nativas de monitoramento em tempo real (AWS Config Rules com remediação via AWS Systems Manager Automation, SCPs no AWS Organizations, e Organization Policies/GCP Security Health Analytics no GCP).
- **Prós:**
  - **Prevenção precoce:** Bloqueia infraestrutura insegura no pipeline de IaC antes que ela exista (Shift-Left Security).
  - **Eficiência e Escala:** Delega para a infraestrutura nativa da AWS e do GCP o monitoramento de conformidade em tempo real, reduzindo drasticamente a carga do `cloud-manager`.
  - **Remediação Automática:** Violações graves (como um bucket S3 exposto ao público) são fechadas automaticamente de forma imediata pelas engines nativas dos provedores de nuvem (ex: Auto-remediation).
- **Contras:**
  - Exige conhecimento da linguagem Rego (para OPA) e das ferramentas nativas de auditoria dos provedores de nuvem.
  - Necessidade de centralizar e consolidar os dados dessas violações para exibição na UI do `cloud-manager` (BFF) conforme especificado na ADR-004.

## Decisão Escolhida
Aprovamos a **Opção B: Uso Combinado de Policy-as-Code Preventivo (OPA/Rego) e Guardrails Nativos Reativos**.
Todas as execuções de baseline descritas na ADR-005 deverão passar por uma etapa de validação estática de conformidade usando Open Policy Agent (OPA) com regras corporativas centralizadas (ex: proibição de IPs `0.0.0.0/0` para portas confidenciais, exigência de chaves KMS geridas pelo cliente para volumes EBS/discos GCP, etc.). Se houver desvio grave, a execução de baseline é abortada imediatamente.
Além disso, o baseline de infraestrutura de cada conta ativará de forma obrigatória as regras de monitoramento contínuo (AWS Config Rules / GCP Security Command Center). Eventos de desvio gerados pelos provedores de nuvem serão publicados em barramentos de eventos locais e roteados para o `cloud-manager` para atualização do painel de vulnerabilidade da conta no Dashboard e notificação das equipes responsáveis.

## Consequências
- **Positivas:**
  - Proteção robusta em camadas (preventiva e reativa).
  - Minimização do risco de vazamento de credenciais e exposição de dados corporativos sensíveis.
  - Governança automatizada e escalável independente do número de contas filhas ativas.
- **Negativas/Riscos:**
  - Complexidade de manter e atualizar bases de regras e scripts Rego.
  - Custos operacionais associados ao uso contínuo de recursos como AWS Config e GCP Security Health Analytics em larga escala.
- **Plano de Mitigação:**
  - Iniciar com um subconjunto enxuto e crítico de regras de segurança (ex: "Top 10 OWASP/CIS Controls") e expandir gradativamente.
  - Usar agregadores nativos dos provedores para centralizar os relatórios em uma única conta de auditoria/segurança, reduzindo o número de requisições e simplificando a ingestão por parte do `cloud-manager`.
