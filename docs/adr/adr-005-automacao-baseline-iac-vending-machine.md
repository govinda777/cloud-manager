# ADR-005: Orquestração de Baseline de Infraestrutura via Terraform/OpenTofu e Vending Machine

- **Status:** Proposto
- **Data:** 2026-07-26
- **Pilar CloudOps:** Automação e Provisionamento de Infraestrutura (IaC e Vending Machine)
- **Relacionado a:** ADR-002, ADR-003

## Contexto e Problema
Após o provisionamento básico de uma conta Cloud (AWS Account ou GCP Project) e a devida vinculação de faturamento (Billing), a conta recém-criada encontra-se vazia, sem recursos essenciais de segurança, redes locais ou permissões base instaladas. Para que uma conta seja declarada como `ACTIVE` com segurança e conformidade, é preciso aplicar um conjunto de baselines de infraestrutura como código (IaC), configurando roles de IAM, políticas locais, buckets de auditoria e conexões de rede padrão de forma automatizada e idempotente no fluxo do `cloud-manager` (CAPE).

## Comparação com ADRs Existentes
Embora a ADR-002 defina a máquina de estados baseada em eventos e a ADR-003 exija a vinculação à conta Seed, o projeto ainda não documentava como o baseline de infraestrutura (recursos reais) seria instanciado e aplicado dentro do ciclo de vida assíncrono do CAPE. Esta ADR especifica a escolha da engine de IaC e como o mecanismo de Vending Machine será disparado para orquestrar os templates de baseline.

## Opções Consideradas

### Opção A: Provisionamento Direto via SDKs Nativos (AWS Java SDK & GCP Cloud Client)
- **Descrição:** Usar código Java puro para chamar diretamente as APIs de criação de recursos (IAM, S3, KMS) via SDK oficial do respectivo provedor de nuvem.
- **Prós:**
  - Evita ferramentas externas ou dependência de binários extras no container do `cloud-manager`.
  - Controle granular e síncrono dos fluxos de erro em Java.
- **Contras:**
  - Complexidade extrema na manutenção e evolução de baselines complexos de infraestrutura.
  - Ausência de mecanismo nativo de gerenciamento de estado (State) e detecção de drift para os recursos do baseline.
  - Viola o princípio de desacoplamento, exigindo reescrita de lógica complexa para cada novo recurso adicionado ao baseline de diferentes nuvens.

### Opção B: Orquestração Baseada em Terraform/OpenTofu com Runner Assíncrono (Self-Hosted ou Terraform Cloud)
- **Descrição:** O `cloud-manager` atua como um coordenador que invoca execuções do Terraform/OpenTofu de forma assíncrona. Os templates de baseline são mantidos em repositórios Git dedicados. O CAPE consome mensagens de fila e dispara uma pipeline CI/CD (ex: GitHub Actions, GitLab CI) ou um runner local em container (via Terraform Worker/Kubernetes Pod) para executar o `terraform apply` com os parâmetros específicos da conta filha criada.
- **Prós:**
  - Utiliza o padrão de mercado mais robusto e consolidado para IaC (Terraform/OpenTofu).
  - Separação clara de responsabilidades: o `cloud-manager` gerencia o ciclo de vida e estado lógico da conta, enquanto o Terraform/OpenTofu gerencia o estado físico dos recursos de infraestrutura.
  - Reutilização de módulos de IaC já validados e desenvolvidos pelas equipes de Cloud/Platform Engineering.
- **Contras:**
  - Introduz complexidade operacional e dependência de um motor de execução externo (pipeline de CI/CD ou runner local).
  - Requer um mecanismo centralizado e seguro para armazenar o `terraform.tfstate` de cada conta filha de forma isolada (ex: buckets S3/GCS individuais com chaves KMS dedicadas).

## Decisão Escolhida
Aprovamos a **Opção B: Orquestração Baseada em Terraform/OpenTofu com Runner Assíncrono**.
O `cloud-manager` (CAPE), no estado `IN_PROVISIONING`, gerará um payload contendo as variáveis específicas da nova conta filha (ID da conta, Região principal, tags globais, etc.) e disparará uma requisição assíncrona (via webhook ou mensagem SQS/PubSub) para um runner de IaC orquestrado (ex: Terraform Cloud, GitLab CI, ou um pod executor Kubernetes do próprio CAPE).
O runner aplicará os módulos Terraform/OpenTofu correspondentes ao baseline corporativo aprovado e, ao concluir, notificará de volta o CAPE via webhook ou fila, permitindo que a conta avance para o estado `BILLING_LINKED` e posteriormente `ACTIVE`. O arquivo de estado do Terraform (`.tfstate`) será obrigatoriamente persistido de forma segura em um bucket de infraestrutura de gerenciamento protegido, usando criptografia KMS e controle de concorrência por DynamoDB/Firestore.

## Consequências
- **Positivas:**
  - Declaração de infraestrutura padronizada e limpa usando HCL.
  - Desacoplamento total do código Java do `cloud-manager` da lógica pesada de IaC.
  - Facilidade de atualização de baselines de segurança (basta alterar o repositório git do módulo Terraform do baseline, sem necessidade de redeploy do backend Java do CAPE).
- **Negativas/Riscos:**
  - Complexidade de integração e necessidade de gerenciamento de webhooks de retorno para notificar o CAPE do sucesso/falha do `terraform apply`.
  - Aumento do tempo total de provisionamento da conta, uma vez que a execução do Terraform pode levar de 3 a 10 minutos.
- **Plano de Mitigação:**
  - Configuração de timeouts generosos no fluxo de mensagens SQS (Dead Letter Queue - DLQ ativa) e resiliência via reprocessamento automático em caso de falha de rede temporária do runner de IaC.
  - Implementação de um BFF/Dashboard (conforme ADR-004) capaz de reportar o log em tempo real das etapas de execução do Terraform para o operador da plataforma.
