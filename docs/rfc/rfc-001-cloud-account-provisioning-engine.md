# RFC 001: Cloud Account Provisioning Engine (CAPE)

**Status:** PROPOSED
**Data:** 2026-07-25
**Autor:** Arquitetura de Software

---

## 1. Resumo Executivo
Esta RFC propõe o design da plataforma CAPE, uma engine orientada a eventos e baseada em Arquitetura Hexagonal para provisionamento e gestão automatizada do ciclo de vida de contas em nuvens públicas (AWS e GCP). O sistema gerenciará contas "Seed" (mães/faturamento central) capazes de desdobrar contas "Filhas" que herdam automaticamente as estruturas organizacionais e fiscais/billing da conta raiz.

## 2. Contexto e Problema
A criação manual de subcontas AWS (via Organizations/Control Tower) e projetos GCP (via Resource Manager) gera gargalos de governança, erros de atribuição de billing e atrasos no Onboarding de equipes. Precisamos de um orquestrador resiliente, assíncrono e desacoplado de provedores de nuvem específicos.

## 3. Arquitetura Proposta (HEDA)
O projeto adotará a arquitetura do repositório `govinda777/hexagonal-event-driven-architecture`:

```plaintext
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
```

### Pacotes Base (`com.govinda777.execution`)
* **`business.model`**: Entidades puras como `CloudAccount`, `AccountState`, `BillingConfig`.
* **`business.gateway`**: Interfaces para persistência, mensageria e SDKs de cloud.
* **`business.logic`**: Regras da máquina de estado (`CreateAccountUseCase`, `LinkBillingUseCase`).
* **`infrastructure`**: Implementações de adaptadores REST, Spring Data JPA, AWS SQS / GCP SDKs e Beans (`UseCaseConfig`).

## 4. Fluxo de Execução Assíncrono

```plaintext
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
```

## 5. Ambiente Local e Ferramental
* **Docker Compose**: Sobe o app Java 21, banco PostgreSQL local e LocalStack para simulação local do AWS SQS e S3.
* **Testes**: Suíte Cucumber (BDD) + JUnit 5 para garantir a transição de estados da máquina.
