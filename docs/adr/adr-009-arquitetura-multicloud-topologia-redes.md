# ADR-009: Arquitetura Multi-Cloud, Multi-Tenant e Topologia de Redes Híbrida

- **Status:** Proposto
- **Data:** 2026-07-26
- **Pilar CloudOps:** Arquitetura Multi-Cloud, Multi-Tenant e Topologia de Redes
- **Relacionado a:** ADR-005

## Contexto e Problema
O provisionamento autônomo de redes (VPCs na AWS e VPC Networks no GCP) para as contas filhas gera o risco de sobreposição de blocos CIDR de rede (IP overlapping). Sem um controle centralizado, as sub-redes criadas aleatoriamente não conseguem se interconectar com o data center corporativo (On-Premises) de forma segura. O ecossistema de infraestrutura do `cloud-manager` deve definir a topologia de rede adequada e como os blocos IP são distribuídos de acordo com o padrão de separação de responsabilidades (ADR-005).

## Comparação com ADRs Existentes
Embora as ADRs anteriores garantam faturamento (ADR-003) e baseline de IaC templatizado (ADR-005), o repositório carecia de uma decisão sobre a topologia de rede e a governança de endereçamento IP (IPAM). Esta ADR formaliza o design de rede corporativo e como o endereçamento de rede é injetado nas contas filhas.

## Opções Consideradas

### Opção A: Definição e Provisionamento de Redes no Backend do CAPE
- **Descrição:** O CAPE seria responsável por rodar regras lógicas de rede no backend Java e disparar chamadas de API nativas diretamente para criar VPCs e tabelas de rotas nas contas filhas.
- **Prós:**
  - Controle síncrono e unificado no backend.
- **Contras:**
  - Forte acoplamento de código de infraestrutura de redes no core lógico do CAPE.
  - Viola a separação de responsabilidades da ADR-005: a criação de recursos físicos deve residir nos templates de repositório de IaC de cada conta, e ser processada pelo **IAC engine**.

### Opção B: Alocação de IPAM pelo CAPE e Provisionamento de Redes via Repositório de IaC e IAC Engine
- **Descrição:** Dividir a responsabilidade de rede do ecossistema:
  1. **CAPE (cloud-manager):** Atua como o cérebro de alocação de IPs. Ele integra-se a um sistema de IPAM corporativo ou pool interno e reserva um bloco CIDR exclusivo e não-sobreposto para a conta filha antes do provisionamento. Este CIDR reservado é inserido como variável no repositório de IaC gerado para a conta filha (ADR-005).
  2. **Pipeline de IaC (via IAC engine):** O template de repositório de IaC da conta contém os recursos lógicos e físicos de rede declarados em código Terraform. O pipeline executado pelo **IAC engine** utiliza a variável CIDR fornecida pelo CAPE para provisionar as VPCs, sub-redes, tabelas de rotas e conexões à rede central corporativa (através de Transit Gateway / VPN Peering).
- **Prós:**
  - Separação perfeita de responsabilidades: o CAPE gerencia o registro e a reserva lógica do IP, enquanto o repositório de IaC da conta cuida de instanciar a topologia de rede física.
  - O código de rede é mantido em formato padrão (HCL Terraform) no repositório IaC, facilitando auditorias e alterações estruturais sem afetar o backend Java do CAPE.
- **Contras:**
  - Dependência na comunicação de parâmetros entre o CAPE (IPAM) e as variáveis de entrada do repositório Git de IaC.

## Decisão Escolhida
Aprovamos a **Opção B: Alocação de IPAM pelo CAPE e Provisionamento de Redes via Repositório de IaC e IAC Engine**.
O `cloud-manager` (CAPE) no estado `IN_PROVISIONING` reserva um bloco de endereços IP exclusivo (ex: `/24`) via IPAM e grava essa informação nas variáveis do repositório Git que foi instanciado a partir do template padrão da conta.
A pipeline do repositório da conta filha será disparada via **IAC engine** e lerá essa variável CIDR para criar a infraestrutura de rede local (VPCs, Subnets) e integrá-la automaticamente à infraestrutura Hub central (através do AWS Transit Gateway Attachment ou GCP Shared VPC / VPN Peering para a Hub Network corporativa).

## Consequências
- **Positivas:**
  - Separação clara de responsabilidades de infraestrutura de rede e código de orquestração de negócios.
  - Escalabilidade sem risco de colisão de IPs corporativos.
  - O pipeline de IaC e o IAC engine cuidam de toda a mecânica pesada e lenta de provisionamento de recursos de redes físicas.
- **Negativas/Riscos:**
  - O provisionamento inicial da conta passa a depender do sucesso das etapas de rede descritas na pipeline de IaC do repositório.
- **Plano de Mitigação:**
  - O template de rede no repositório de IaC deve incluir etapas rígidas de validação de conectividade interna (Lints de rede e testes unitários de IaC) e retornar códigos de erro claros via pipeline em caso de problemas, notificando o CAPE do status.
