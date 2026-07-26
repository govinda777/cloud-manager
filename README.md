Com certeza! Vamos estruturar os artefatos de arquitetura necessários (Prompt de Especificação, RFC e ADRs) rigorosamente alinhados com o projeto Hexagonal Event-Driven Architecture (HEDA) fornecido.
📄 Parte 1: Prompt Master para LLMs de Código (Copilot/Claude/GPT-4)
Use o prompt abaixo para alimentar sua IA de codificação para gerar o boilerplate e implementação inicial do seu projeto:



Plaintext
Atue como Engenheiro de Software Principal especialista em Arquitetura Hexagonal e Event-Driven Systems.
Crie um microsserviço completo em Java 21 e Spring Boot 3.2+ com o objetivo de orquestrar a criação e gerenciamento de Contas Cloud (AWS Accounts e GCP Projects).

Siga rigorosamente a arquitetura do repositório 'govinda777/hexagonal-event-driven-architecture':

1. ISOLAMENTO DO DOMÍNIO (business):
   - O pacote 'business' deve ser JAVA PURO, sem dependências de frameworks (sem Spring, Jackson, AWS SDK, GCP SDK, JPA).
   - Sem herança de classes; use composição e interfaces em 'gateway'.
   - O fluxo é orquestrado via State Machine e acionado por eventos de fila (SQS/LocalStack).
   - Registre Beans do Spring programaticamente em 'UseCaseConfig.java' na infraestrutura, sem anotações de DI no núcleo funcional.

2. DOMÍNIO E NÚCLEO DE NEGÓCIO:
   - Contas Seed (Root Billing Accounts): AWS Management Account e GCP Billing Account/Organization Root.
   - Contas Filhas: Devem ser vinculadas obrigatoriamente a uma conta Seed e herdar suas configurações de faturamento/billing.
   - Estados da Conta (AccountState): CREATED, IN_PROVISIONING, BILLING_LINKED, ACTIVE, FAILED.

3. INFRAESTRUTURA E MENSAGERIA LOCAL:
   - Forneça um setup funcional via 'docker-compose.yml' contendo LocalStack (para AWS SQS) e Local GCP Pub/Sub / Mocks.
   - Forneça Dockerfile multi-stage funcional para Java 21.

Gere a estrutura de pacotes, os DTOs, Entidades de Domínio, Casos de Uso (CreateAccountUseCase, ProcessAccountProvisioningUseCase) e Mapeadores.


📜 Parte 2: RFC (Request for Comments)
RFC-001: Cloud Account Provisioning Engine (CAPE)
Status: PROPOSED
Data: 2026-07-25
Autor: Arquitetura de Software
1. Resumo Executivo
Esta RFC propõe o design da plataforma CAPE, uma engine orientada a eventos e baseada em Arquitetura Hexagonal para provisionamento e gestão automatizada do ciclo de vida de contas em nuvens públicas (AWS e GCP). O sistema gerenciará contas "Seed" (mães/faturamento central) capazes de desdobrar contas "Filhas" que herdam automaticamente as estruturas organizacionais e fiscais/billing da conta raiz.
2. Contexto e Problema
A criação manual de subcontas AWS (via Organizations/Control Tower) e projetos GCP (via Resource Manager) gera gargalos de governança, erros de atribuição de billing e atrasos no Onboarding de equipes. Precisamos de um orquestrador resiliente, assíncrono e desacoplado de provedores de nuvem específicos.
3. Arquitetura Proposta (HEDA)
O projeto adotará a arquitetura do repositório govinda777/hexagonal-event-driven-architecture:



Plaintext
               +--------------------------------------------------+
               |            INFRASTRUCTURE (Adapters)             |
               |  +--------------------+  +--------------------+  |
  HTTP Request |  |   REST Controller  |  | SQS/PubSub Listener|  |
-------------> |  +---------+----------+  +---------+----------+  |
               |            |                       |             |
               |            v                       v             |
               |  +--------------------------------------------+  |
               |  |           INPUT PORTS (Use Cases)          |  |
               |  +---------------------+----------------------+  |
               |                        |                         |
               |  +---------------------v----------------------+  |
               |  |          DOMAIN CORE (Pure Java)           |  |
               |  |  - AccountEntity      - BillingPolicy      |  |
               |  |  - AccountState Enum  - StateMachine Logic |  |
               |  +---------------------+----------------------+  |
               |                        |                         |
               |  +---------------------v----------------------+  |
               |  |          OUTPUT PORTS (Gateways)           |  |
               |  +---------------------+----------------------+  |
               |            |                       |             |
               |  +---------v----------+  +---------v----------+  |
               |  |   Database Adapter |  | Cloud SDK Adapter  |  |
               |  +--------------------+  +--------------------+  |
               +--------------------------------------------------+


Pacotes Base (com.govinda777.execution):
business.model: Entidades puras como CloudAccount, AccountState, BillingConfig.
business.gateway: Interfaces para persistência, mensageria e SDKs de cloud.
business.logic: Regras da máquina de estado (CreateAccountUseCase, LinkBillingUseCase).
infrastructure: Implementações de adaptadores REST, Spring Data JPA, AWS SQS / GCP SDKs e Beans (UseCaseConfig).
4. Fluxo de Execução Assíncrono



Plaintext
[POST /accounts] -> State: CREATED -> Publishes SQS Event
                          |
                          v
               [SQS Queue: account-provisioning-queue]
                          |
                          v
       [Consume Event -> Process Provisioning (AWS/GCP)]
                          |
               +----------+----------+
               |                     |
          (Success)               (Error)
               |                     |
               v                     v
     State: BILLING_LINKED     State: FAILED (Save History Log)
               |
               v
         State: ACTIVE


5. Ambiente Local e Ferramental
Docker Compose: Sobe o app Java 21, banco PostgreSQL local e LocalStack para simulação local do AWS SQS e S3.
Testes: Suíte Cucumber (BDD) + JUnit 5 para garantir a transição de estados da máquina.
🏛 Parte 3: ADRs (Architecture Decision Records)
ADR-001: Adopção de Arquitetura Hexagonal com Domínio em Java Puro
Status: Aprovado
Contexto: Necessitamos isolar as regras de negócio de governança de nuvem da volatilidade de SDKs externos (AWS SDK, GCP SDK) e frameworks (Spring Boot, Hibernate).
Decisão: O núcleo de domínio (business) será escrito estritamente em Java 21 puro sem anotações de terceiros. Toda injeção de dependências dos Casos de Uso será feita explicitamente via Java Config (UseCaseConfig.java).
Consequências Positivas: Testabilidade total sem Spring Context, alta manutenibilidade, substituição simples de integradores de nuvem.
Consequências Negativas: Necessidade de mapeamento bidirecional de dados entre entidades de banco (JpaEntity) e domínio (Entity).
ADR-002: Orquestração Baseada em Eventos com Máquina de Estados
Status: Aprovado
Contexto: A criação de contas na AWS/GCP é um processo demorado (operações assíncronas de infraestrutura). A chamadas síncronas HTTP são inviáveis.
Decisão: Adotar transições de estado orientadas a eventos utilizando AWS SQS. As etapas passam de forma estrita por CREATED $\rightarrow$ IN_PROVISIONING $\rightarrow$ BILLING_LINKED $\rightarrow$ ACTIVE. Em caso de falha, transita para FAILED gravando o historyLog de erro.
Consequências Positivas: Resiliência contra timeouts, desacoplamento e idempotência de execução.
Consequências Negativas: Complexidade no acompanhamento eventual do estado da conta pelo cliente final.
ADR-003: Herança de Billing para Contas Filhas via Contas Seed
Status: Aprovado
Contexto: Nenhuma conta filha (AWS Account / GCP Project) pode ser órfã de faturamento.
Decisão: O evento de criação deve exigir o identificador da conta Seed correspondente (seedAccountId). A etapa BILLING_LINKED fará a associação automática via API da AWS (Organizations AttachPolicy/Billing) e GCP (Cloud Billing API) antes de marcar a conta como ACTIVE.
Consequências Positivas: Garantia de conformidade orçamentária e automação total de governança.
Consequências Negativas: Dependência direta da integridade e cota disponível da conta Seed.
