.DEFAULT_GOAL := help
.PHONY: help setup init env up down restart test test-integration test-bdd clean logs status

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

up-all: env ## Sobe toda a pilha, incluindo a aplicação Spring Boot contêinerizada
	docker compose --profile full up -d --build

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

## @ Limpeza
clean: down ## Limpa contêineres, volumes Docker, caches e o diretório de build target/
	docker compose down -v --remove-orphans
	@if command -v java >/dev/null 2>&1; then ./mvnw clean; else docker run --rm -v $$(pwd):/app -w /app maven:3.9.6-eclipse-temurin-21-alpine mvn clean; fi
	@rm -rf .tmp target/

