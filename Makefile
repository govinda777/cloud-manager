.DEFAULT_GOAL := help
.PHONY: help setup init env up down restart test test-integration test-bdd clean logs status config aws gcp disaster-recovery

# Permite passar argumentos para o comando config (ex: make config aws)
ifeq ($(firstword $(MAKECMDGOALS)),config)
  CONFIG_ARGS := $(wordlist 2,$(words $(MAKECMDGOALS)),$(MAKECMDGOALS))
  $(eval $(CONFIG_ARGS):;@:)
endif

# Variáveis
ENV_FILE ?= .env
ENV_EXAMPLE ?= .env.example
TAGS ?= "not @wip"

## @ Help
help: ## Exibe os comandos disponíveis no Makefile
	@echo "Uso: make [alvo] [TAGS=\"@smoke\"]"
	@echo ""
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'

## @ Setup & Inicialização
setup: env init ## Inicializa o ambiente local completo (gera .env e baixa dependências)

config: ## Configura recursos específicos do projeto (ex: make config aws | gcp)
	@if [ "$(CONFIG_ARGS)" = "aws" ]; then \
		bash .agents/skills/ponta-a-ponta/scripts/config-aws.sh; \
	elif [ "$(CONFIG_ARGS)" = "gcp" ]; then \
		bash .agents/skills/ponta-a-ponta/scripts/config-gcp.sh; \
	else \
		echo "Uso: make config [aws|gcp]"; \
	fi

env: ## Cria o arquivo .env local a partir do .env.example se não existir
	@if [ ! -f $(ENV_FILE) ]; then \
		echo "Criando $(ENV_FILE) a partir de $(ENV_EXAMPLE)..."; \
		cp $(ENV_EXAMPLE) $(ENV_FILE); \
	else \
		echo "$(ENV_FILE) já existe."; \
	fi

init: ## Torna o wrapper executável e baixa as dependências Maven em modo offline
	@chmod +x mvnw
	@./mvnw dependency:go-offline -B

## @ Gerenciamento da Infraestrutura
up: env ## Sobe os serviços de infraestrutura local em background (PostgreSQL e ElasticMQ)
	docker compose up -d postgres elasticmq
	@if [ -f .env ]; then \
		export $$(grep -v '^#' .env | xargs); \
	fi; \
	printf "\n\033[1;36m========================================================================\033[0m\n"; \
	printf "\033[1;32m🚀 Cloud Manager - Serviços de Infraestrutura Iniciados!\033[0m\n"; \
	printf "\033[1;36m========================================================================\033[0m\n"; \
	printf "\033[1;34m☁️  PROVEDORES DE INFRAESTRUTURA (LOCAL MOCKS):\033[0m\n"; \
	printf "  - \033[1mPostgreSQL:\033[0m      localhost:$${POSTGRES_PORT:-5432} (DB: $${POSTGRES_DB:-cloud_manager_db}, User: $${POSTGRES_USER:-cloud_user})\n"; \
	printf "  - \033[1mElasticMQ (SQS):\033[0m localhost:$${ELASTICMQ_PORT:-9324} (AWS SQS Mock API)\n"; \
	printf "  - \033[1mSQS Console UI:\033[0m  http://localhost:$${ELASTICMQ_UI_PORT:-9325} (Interface Web da Fila)\n"; \
	printf "\033[1;36m========================================================================\033[0m\n"

up-all: env ## Sobe toda a pilha, incluindo a aplicação Spring Boot contêinerizada
	docker compose --profile full up -d --build
	@if [ -f .env ]; then \
		export $$(grep -v '^#' .env | xargs); \
	fi; \
	printf "\n\033[1;36m========================================================================\033[0m\n"; \
	printf "\033[1;32m🚀 Cloud Manager - Aplicação e Infraestrutura Iniciadas com Sucesso!\033[0m\n"; \
	printf "\033[1;36m========================================================================\033[0m\n"; \
	printf "\033[1;34m💻 APLICAÇÃO PRINCIPAL:\033[0m\n"; \
	printf "  - \033[1mAPI Base:\033[0m        http://localhost:8080\n"; \
	printf "  - \033[1mSwagger UI:\033[0m      http://localhost:8080/swagger-ui.html\n"; \
	printf "  - \033[1mOpenAPI Docs:\033[0m    http://localhost:8080/api-docs\n\n"; \
	printf "\033[1;34m📊 ADMINISTRAÇÃO & OBSERVABILIDADE:\033[0m\n"; \
	printf "  - \033[1mBusiness Dashboard:\033[0m http://localhost:8080/accounts/dashboard (Métricas em tempo real)\n"; \
	printf "  - \033[1mHealth Check:\033[0m       http://localhost:8080/actuator/health (Status da Plataforma)\n"; \
	printf "  - \033[1mMetrics Endpoint:\033[0m   http://localhost:8080/actuator/metrics (Métricas do Sistema)\n\n"; \
	printf "\033[1;34m☁️  PROVEDORES DE INFRAESTRUTURA (LOCAL MOCKS):\033[0m\n"; \
	printf "  - \033[1mPostgreSQL:\033[0m      localhost:$${POSTGRES_PORT:-5432} (DB: $${POSTGRES_DB:-cloud_manager_db}, User: $${POSTGRES_USER:-cloud_user})\n"; \
	printf "  - \033[1mElasticMQ (SQS):\033[0m localhost:$${ELASTICMQ_PORT:-9324} (AWS SQS Mock API)\n"; \
	printf "  - \033[1mSQS Console UI:\033[0m  http://localhost:$${ELASTICMQ_UI_PORT:-9325} (Interface Web da Fila)\n"; \
	printf "\033[1;36m========================================================================\033[0m\n"

down: ## Derruba todos os contêineres e limpa as redes
	docker compose --profile full down

restart: down up ## Reinicia os serviços de infraestrutura local

status: ## Exibe o status dos contêineres em execução
	docker compose ps

logs: ## Exibe os logs dos contêineres em tempo real
	docker compose logs -f

## @ Suíte de Testes
test: ## Executa os testes unitários do projeto (detecta Java ou roda no Docker com cache)
	@if command -v java >/dev/null 2>&1; then \
		./mvnw test -Dtest="*Test" -DfailIfNoTests=false; \
	else \
		docker run --rm -v $$(pwd):/app -v ~/.m2:/root/.m2 -w /app maven:3.9.6-eclipse-temurin-21-alpine mvn test -Dtest="*Test" -DfailIfNoTests=false; \
	fi

test-integration: up ## Executa os testes de integração (detecta Java ou roda no Docker com cache)
	@if command -v java >/dev/null 2>&1; then \
		./mvnw test-compile failsafe:integration-test failsafe:verify; \
	else \
		docker run --rm -v $$(pwd):/app -v ~/.m2:/root/.m2 -w /app --network cloud-manager_cloud-manager-net -e SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/cloud_manager_db -e AWS_SQS_ENDPOINT=http://elasticmq:9324 maven:3.9.6-eclipse-temurin-21-alpine mvn test-compile failsafe:integration-test failsafe:verify; \
	fi

test-bdd: up ## Executa os cenários BDD/Cucumber (detecta Java ou roda no Docker com cache)
	@if command -v java >/dev/null 2>&1; then \
		./mvnw test -Dcucumber.filter.tags=$(TAGS); \
	else \
		docker run --rm -v $$(pwd):/app -v ~/.m2:/root/.m2 -w /app --network cloud-manager_cloud-manager-net -e SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/cloud_manager_db -e AWS_SQS_ENDPOINT=http://elasticmq:9324 maven:3.9.6-eclipse-temurin-21-alpine mvn test -Dcucumber.filter.tags=$(TAGS); \
	fi

disaster-recovery: up ## Executa a bateria de testes de destruição e criação do Disaster Recovery (Playwright e BDD)
	@echo "🔥 [DR] Iniciando bateria de testes de Disaster Recovery..."
	@echo "👉 [Passo 0] Executando destruição prioritária nos provedores cloud..."
	-npx playwright test tests/dr-cleanup-clouds.spec.ts
	@echo "👉 [Passo 0] Limpando registros obsoletos de contas base no banco de dados..."
	-docker exec -i cloud-manager-postgres psql -U cloud_user -d cloud_manager_db -c "DELETE FROM accounts WHERE name LIKE '%Seed%' OR name LIKE '%Master%';"
	@echo "👉 [Passo 1 & 2] Criando novas contas root e configurando APIs (Playwright)..."
	-npx playwright test tests/dr-aws-registration.spec.ts tests/dr-gcp-setup.spec.ts tests/dr-aws-bootstrap-iam.spec.ts tests/dr-gcp-bootstrap-sa.spec.ts
	@echo "👉 [Passo 3 & 4] Rodando suíte de testes BDD para certificar a integridade do DR..."
	@if command -v java >/dev/null 2>&1; then \
		./mvnw test -Dtest=CucumberTestRunner; \
	else \
		docker run --rm -v $$(pwd):/app -v ~/.m2:/root/.m2 -w /app --network cloud-manager_cloud-manager-net -e SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/cloud_manager_db -e AWS_SQS_ENDPOINT=http://elasticmq:9324 maven:3.9.6-eclipse-temurin-21-alpine mvn test -Dtest=CucumberTestRunner; \
	fi
	@echo "🚀 [DR] Bateria de testes de Disaster Recovery concluída!"

## @ Limpeza
clean: down ## Limpa contêineres, volumes Docker, caches e o diretório de build target/
	docker compose down -v --remove-orphans
	@if command -v java >/dev/null 2>&1; then ./mvnw clean; else docker run --rm -v $$(pwd):/app -w /app maven:3.9.6-eclipse-temurin-21-alpine mvn clean; fi
	@rm -rf .tmp target/

