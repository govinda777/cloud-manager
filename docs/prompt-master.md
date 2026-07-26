# Prompt Master para LLMs de Código (Copilot/Claude/GPT-4)

Use o prompt abaixo para alimentar sua IA de codificação para gerar o boilerplate e a implementação inicial do seu projeto:

```plaintext
Atue como Engenheiro de Software Principal especialista em Arquitetura Hexagonal e Event-Driven Systems.
Crie um microsserviço completo em Java 21 e Spring Boot 3.2+ com o objetivo de orquestrar a criação e gerenciamento de Contas Cloud (AWS Accounts e GCP Projects).

Siga rigorosamente a arquitetura do repositório 'govinda777/hexagonal-event-driven-architecture':

1. ISOLAMENTO DO DOMÍNIO (business):
   - O pacote 'business' deve ser JAVA PURO, sem dependências de frameworks (sem Spring, Jackson, AWS SDK, GCP SDK, JPA).
   - Sem herança de classes; use composição e interfaces em 'gateway'.
   - O fluxo é orquestrado via State Machine e acionado por eventos de fila (SQS/LocalStack).
   - Registre Beans do Spring programaticamente in 'UseCaseConfig.java' na infraestrutura, sem anotações de DI no núcleo funcional.

2. DOMÍNIO E NÚCLEO DE NEGÓCIO:
   - Contas Seed (Root Billing Accounts): AWS Management Account e GCP Billing Account/Organization Root.
   - Contas Filhas: Devem ser vinculadas obrigatoriamente a uma conta Seed e herdar suas configurações de faturamento/billing.
   - Estados da Conta (AccountState): CREATED, IN_PROVISIONING, BILLING_LINKED, ACTIVE, FAILED.

3. INFRAESTRUTURA E MENSAGERIA LOCAL:
   - Forneça um setup funcional via 'docker-compose.yml' contendo LocalStack (para AWS SQS) e Local GCP Pub/Sub / Mocks.
   - Forneça Dockerfile multi-stage funcional para Java 21.

Gere a estrutura de pacotes, os DTOs, Entidades de Domínio, Casos de Uso (CreateAccountUseCase, ProcessAccountProvisioningUseCase) e Mapeadores.
```
