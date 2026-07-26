# ADR-009: Arquitetura Multi-Cloud, Multi-Tenant e Topologia de Redes Híbrida

- **Status:** Proposto
- **Data:** 2026-07-26
- **Pilar CloudOps:** Arquitetura Multi-Cloud, Multi-Tenant e Topologia de Redes
- **Relacionado a:** ADR-005

## Contexto e Problema
O provisionamento autônomo de redes (VPCs na AWS e VPC Networks no GCP) para as contas filhas gera o risco iminente de sobreposição de blocos CIDR de rede (IP overlapping). Sem um controle centralizado, as sub-redes criadas aleatoriamente não conseguem se interconectar com o data center corporativo (On-Premises) ou com outras contas filhas de forma segura e direta, inviabilizando integrações multi-tenant cruciais e gerando complexidade extrema no gerenciamento de rotas e segurança de borda.

## Comparação com ADRs Existentes
Embora as ADRs anteriores garantam faturamento (ADR-003) e baseline de IaC (ADR-005), o repositório carecia de uma decisão sobre a topologia de rede e a governança de endereçamento IP (IPAM) para o ecossistema multi-cloud. Esta ADR formaliza o design de rede corporativo e a conectividade segura inter-contas.

## Opções Consideradas

### Opção A: Redes Isoladas sem Comunicação Direta (Peering de VPCs Sob Demanda)
- **Descrição:** Cada conta filha cria sua VPC de forma independente. Conexões de rede inter-contas ou com o On-Premises são feitas sob demanda configurando peering manual de VPC (VPC Peering) ou VPNs pontuais.
- **Prós:**
  - Desacoplamento inicial de redes.
  - Baixo custo fixo (sem gateways centrais de tráfego de rede).
- **Contras:**
  - Extremamente difícil de gerenciar conforme a escala aumenta (cria uma complexa "teia de aranha" de peerings difíceis de rastrear).
  - Alto risco de sobreposição de blocos CIDR (IPs iguais), impossibilitando o estabelecimento de peering.
  - Limitações de escala nos provedores (limites físicos de conexões de peering por VPC).

### Opção B: Topologia Hub-and-Spoke com Gerenciamento Centralizado de IPAM via Transit Gateway / Cloud Interconnect
- **Descrição:** Adotar um modelo de topologia de rede centralizada tipo "Hub-and-Spoke":
  1. **Rede Hub (Central):** Uma conta/projeto de rede centralizadora hospeda o gateway de tráfego e conexões corporativas híbridas (AWS Transit Gateway na AWS, e Cloud Router / Shared VPC Host Project no GCP) interligada ao data center físico por links dedicados (Direct Connect / Cloud Interconnect).
  2. **Redes Spoke (Filhas):** VPCs das contas filhas são conectadas de forma automática à Hub Central durante o provisionamento.
  3. **IPAM (IP Address Management):** O `cloud-manager` (CAPE) integra-se a um serviço central de IPAM (AWS IPAM ou um IPAM corporativo terceiro via API) no momento de registrar uma nova conta, solicitando e reservando um bloco CIDR exclusivo e não-sobreposto para a nova VPC filha antes de disparar o baseline Terraform.
- **Prós:**
  - Eliminação definitiva de sobreposição de blocos CIDR nas sub-redes corporativas.
  - Segurança de borda centralizada (inspeção de tráfego leste-oeste por firewalls dedicados na rede Hub).
  - Conectividade automática e instantânea das aplicações filhas com serviços internos no data center.
- **Contras:**
  - Custos fixos elevados para manter gateways centrais de trânsito de redes e links dedicados.
  - A rede Hub representa um ponto centralizado de tráfego que requer monitoramento contínuo de capacidade e cotas.

## Decisão Escolhida
Aprovamos a **Opção B: Topologia Hub-and-Spoke com Gerenciamento Centralizado de IPAM via Transit Gateway / Cloud Interconnect**.
Durante o processo de provisionamento (estado `IN_PROVISIONING`), o `cloud-manager` fará uma chamada API interna de IPAM para reservar um bloco de endereços IP exclusivos de tamanho pré-definido (ex: `/24` ou `/22` conforme a necessidade do projeto cadastrado). Esse bloco CIDR será passado como variável obrigatória de entrada para os scripts de baseline do Terraform (ADR-005).
O baseline do Terraform criará as VPCs/Redes locais utilizando o CIDR reservado e conectará automaticamente essa rede à infraestrutura central da empresa (através do AWS Transit Gateway Attachment ou GCP Shared VPC / VPN Peering para a Hub Network corporativa).

## Consequências
- **Positivas:**
  - Escalabilidade infinita de rede sem risco de colisões de IPs.
  - Facilidade de roteamento unificado, inspeção de segurança e compliance de tráfego de rede de dados.
  - Provisionamento de conexões de rede robustas sem esforço manual das equipes de projeto.
- **Negativas/Riscos:**
  - Complexidade acrescida no provisionamento inicial do baseline, que passa a depender de APIs de rede corporativas estáveis.
  - Custo associado à transferência de dados através de roteadores de trânsito centrais de rede (Transit Gateway / Interconnect).
- **Plano de Mitigação:**
  - Manter um pool de IPs de contingência ("Buffer CIDR Pools") localmente em banco para o caso de indisponibilidade temporária de APIs externas do IPAM principal corporativo.
  - Implementar alertas severos de capacidade e exaustão de IPs nos pools de redes das regiões ativas no Dashboard.
