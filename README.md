# Cloud Manager / CAPE (Cloud Account Provisioning Engine)

O **Cloud Manager** (ou CAPE - Cloud Account Provisioning Engine) é uma plataforma para provisionamento e gestão automatizada do ciclo de vida de contas em nuvens públicas (AWS e GCP).

Desenvolvido utilizando **Java 21**, **Spring Boot 3.2+** e seguindo rigorosamente os padrões de **Arquitetura Hexagonal Orientada a Eventos (HEDA)**, o sistema isola completamente as regras de negócio de governança de nuvem da volatilidade de SDKs externos e frameworks de persistência ou mensageria.

---

## 🏛️ Arquitetura do Projeto

O projeto adota o design de arquitetura hexagonal baseado no repositório `govinda777/hexagonal-event-driven-architecture`:

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

### Princípios do Design
1. **Isolamento de Domínio (`business`)**: O núcleo da aplicação é escrito estritamente em **Java Puro**, livre de dependências externas como annotations de frameworks (Spring, Jackson, JPA/Hibernate, SDKs AWS ou GCP).
2. **Injeção de Dependência Programática**: Todos os Beans de Casos de Uso do Spring são declarados programaticamente em `UseCaseConfig.java` na camada de infraestrutura.
3. **Máquina de Estados e Resiliência**: O ciclo de vida de uma conta é controlado assincronamente por meio de uma máquina de estados robusta guiada por eventos (via AWS SQS).

---

## 🔄 Fluxo de Provisionamento

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

### Estados da Conta (`AccountState`)
- `CREATED`: Conta criada na base e aguardando processamento.
- `IN_PROVISIONING`: Em processo de criação no provedor de nuvem correspondente.
- `BILLING_LINKED`: Conta provisionada com sucesso e vinculada à conta **Seed** correspondente.
- `ACTIVE`: Conta totalmente funcional e pronta para uso.
- `FAILED`: Falha na execução em qualquer etapa do fluxo (com log detalhado de erro no histórico).

---

## 📂 Estrutura de Pastas e Documentação

Para aprofundar-se nas especificações técnicas e decisões que moldaram este projeto, consulte a nossa pasta de documentações:

* 📄 **[RFC-001 (Request for Comments)](docs/rfc/rfc-001-cloud-account-provisioning-engine.md)**: Detalhamento do design da plataforma CAPE, modelagem de dados e estratégias de integração.
* 🏛️ **ADRs (Architecture Decision Records)**:
  * **[ADR-001 - Adoção de Arquitetura Hexagonal com Domínio em Java Puro](docs/adr/adr-001-arquitetura-hexagonal-dominio-puro.md)**
  * **[ADR-002 - Orquestração Baseada em Eventos com Máquina de Estados](docs/adr/adr-002-orquestracao-eventos-maquina-estados.md)**
  * **[ADR-003 - Herança de Faturamento para Contas Filhas via Contas Seed](docs/adr/adr-003-heranca-billing-contas-filhas.md)**
* 🤖 **[Prompt Master para IAs](docs/prompt-master.md)**: Prompt de engenharia estruturado para acelerar o desenvolvimento de novas rotas ou geração de boilerplates e modelos complementares utilizando Copilot, Claude ou GPT-4.

---

## 🛠️ Como Executar Localmente

### Pré-requisitos
- **Java 21**
- **Docker** e **Docker Compose**
- **Maven** (ou wrapper embarcado `./mvnw`)

### Setup do Ambiente Local
A infraestrutura local é orquestrada via Docker Compose, que disponibiliza o banco PostgreSQL e o LocalStack (simulando o AWS SQS):

```bash
# Iniciar dependências locais (Banco de Dados e SQS local)
docker-compose up -d
```

### Compilar e Rodar a Aplicação
```bash
# Executar testes unitários e de integração
./mvnw clean test

# Inicializar o microsserviço Spring Boot
./mvnw spring-boot:run
```

---

## 🧪 Suíte de Testes
O projeto emprega **JUnit 5** juntamente com cenários em formato BDD via **Cucumber** para garantir a consistência das transições da máquina de estado perante variados fluxos e erros de rede.
