---
name: disaster-recovery
description: Guia de Disaster Recovery extremo para o Cloud Manager, detalhando o processo completo de recriação de contas base na nuvem (AWS e GCP), mapeamento de informações necessárias (e-mails, permissões) e passos para restabelecimento da operação.
---

# Skill: Disaster Recovery Extremo (Novas Contas Cloud)

Esta skill orienta o procedimento de **Disaster Recovery (DR) extremo** para o **Cloud Manager**, especificamente nos cenários onde as contas base (Master Seed) da AWS e/ou GCP foram completamente comprometidas, deletadas ou desativadas, exigindo o provisionamento de novas contas do zero para restabelecer a infraestrutura e a capacidade de vending machine.

---

## 1. Avaliação de Informações Necessárias (Checklist de Requisitos)

Antes de iniciar o processo de reconstrução, é mandatório coletar e validar as seguintes informações básicas e credenciais estruturais:

### 1.1. Detalhes de E-mail de Registro (Donos das Contas Base)
Estas contas de e-mail são usadas como proprietárias raiz (root accounts) das novas organizações e devem ser lidas a partir do arquivo `.env` local para evitar vazamentos de informações sensíveis:
* **AWS Root/Master Seed Email:** Carregado a partir da variável `${AWS_ROOT_EMAIL}` (ex: `govinda777@protonmail.com`)
* **GCP Root/Master Seed Email:** Carregado a partir da variável `${GCP_ROOT_EMAIL}` (ex: `govinda777.protonmail@gmail.com`)

### 1.2. AWS (Amazon Web Services)
Para reconfigurar a AWS sob uma nova conta base:
1. **Acesso ao Console Root:** Acesso de login usando o e-mail definido em `${AWS_ROOT_EMAIL}` para a criação da Organização AWS (caso esteja criando do zero).
2. **AWS Organization ID:** O ID da nova organização criada.
3. **AWS IAM Role / STS Credentials:**
   * Uma Role IAM administrativa na nova conta base confiando no OIDC/IAM do Cloud Manager.
   * Access Key ID e Secret Access Key temporárias ou Role ARN para configuração da pipeline.
4. **Billing/Payment Setup:** Configuração de pagamento ativa na nova conta raiz (necessário para criar subcontas via Organizations API).

### 1.3. GCP (Google Cloud Platform)
Para reconfigurar o GCP sob uma nova conta base:
1. **Acesso ao Console Administrador:** Login com a conta definida em `${GCP_ROOT_EMAIL}`.
2. **GCP Organization ID:** ID da organização ou pasta raiz no Google Cloud Console.
3. **GCP Billing Account ID:** Um ID de conta de faturamento ativo no GCP que possa ser associado aos novos projetos gerados.
4. **Service Account Principal:** Chave JSON de uma Conta de Serviço (Service Account) com permissões de `Folder Admin`, `Project Creator` e `Billing User` na raiz da organização.

### 1.4. Variáveis de Ambiente do Cloud Manager
Para que o sistema passe a gerenciar as novas contas, as seguintes variáveis de ambiente no arquivo `.env` (ou no Secrets Manager) devem ser reavaliadas e atualizadas:
* `AWS_ACCESS_KEY_ID` e `AWS_SECRET_ACCESS_KEY` (apontando para a nova conta raiz).
* `AWS_ROLE_ARN` (para assumir permissões de administrador na conta raiz).
* `GCP_PROJECT_ID` (projeto de administração na nova estrutura).
* `GCP_CREDENTIALS_JSON` (conteúdo JSON da Service Account da nova organização).
* `GCP_BILLING_ACCOUNT` (ID da nova conta de faturamento).

**Dados de Cartão / Faturamento (Utilizados apenas nos passos manuais de registro)**:
* `BILLING_CARD_NUMBER` (Número do cartão de crédito para faturamento).
* `BILLING_CARD_EXPIRY` (Data de expiração do cartão).
* `BILLING_CARD_CVV` (Código de segurança CVV).
* `BILLING_HOLDER_NAME` (Nome impresso no cartão do titular).

### 1.5. Mapeamento de Etapas: Manuais vs. Automatizadas

Para planejar a execução do Disaster Recovery de forma eficiente, a tabela abaixo categoriza a natureza de cada atividade com a automação via **Playwright**:

| Etapa | Tipo | Descrição / Ação Necessária |
| :--- | :--- | :--- |
| **Passo 0: Destruição e Exclusão** | **Misto** | **Automatizado (Playwright) [Prioritário]:** Exclusão imediata das organizações/projetos legados nos consoles das nuvens.<br>**Manual:** Limpeza secundária no banco de dados via queries SQL para remover seeds duplicadas. |
| **Passo 1: Criação de Contas Root** | **Automatizado (Playwright)** | **Automatizado:** Execução do script Playwright para cadastro inicial via console web das contas root (usando os e-mails `${AWS_ROOT_EMAIL}` e `${GCP_ROOT_EMAIL}`), inserindo dados de cartão do `.env` e aceitando os termos. |
| **Passo 2: Acessos de API** | **Automatizado (Playwright)** | **Automatizado:** Criação da Service Account raiz do GCP, exportação do JSON de credenciais, ativação de APIs e criação da IAM Role de Bootstrap na conta AWS. |
| **Configuração de Variáveis** | **Manual** | **Manual:** Configuração/Validação inicial das chaves no arquivo `.env`. |
| **Passo 3: Atualização do Banco** | **Automatizado** | **Automatizado:** Execução de scripts SQL de atualização dos IDs das novas sementes na tabela `cloud_account`. |
| **Passo 4: Smoke Tests** | **Automatizado** | **Automatizado:** Validação das credenciais via scripts automáticos de CI/CD ou CLI (`aws sts` / `gcloud auth`). |
| **Passo 5: Vending & Pool** | **Automatizado** | **Automatizado:** Geração de novas contas filhas (pool) a partir do novo seed disparado por SQS ou chamada de API. |

---

## 2. Passo a Passo do Processo de Recuperação (Caminho Completo)

Siga rigorosamente as etapas abaixo para realizar o Disaster Recovery de forma segura.

### Passo 0: Identificação e Exclusão das Contas Base Existentes (Cleanup)
Antes de criar novas contas, é fundamental identificar o estado das contas semente atuais e realizar a limpeza no banco de dados e nos provedores de nuvem para evitar duplicidades e conflitos de chaves (como contas em estado `FAILED` ou duplicadas no dashboard).

1. **Destruição/Cleanup de Recursos Órfãos no Provedor Cloud (PRIMEIRA ETAPA OBRIGATÓRIA):**
   A primeira ação do Disaster Recovery deve ser a eliminação das contas e organizações base legadas diretamente nos provedores cloud para liberar e-mails, domínios e chaves. Execute o script Playwright de cleanup:
   ```bash
   npx playwright test tests/dr-cleanup-clouds.spec.ts
   ```

2. **Identificar contas no Banco de Dados:**
   Execute uma consulta para listar todas as contas semente cadastradas:
   ```sql
   SELECT id, name, email, provider, state, created_at 
   FROM cloud_account 
   WHERE name LIKE '%Seed%' OR name LIKE '%Master%';
   ```

3. **Remover/Excluir contas semente antigas ou inconsistentes do Banco de Dados:**
   Se houver registros duplicados ou em estado `FAILED` (como múltiplos `AWS-Master-Seed` com e-mails corporativos legados), delete ou desative-os para limpar a base:
   ```sql
   -- Opção A: Excluir fisicamente (se não houver dependências de chaves estrangeiras ativas)
   DELETE FROM cloud_account WHERE id = 'ID_DA_CONTA_ANTIGA';

   -- Opção B: Desativar e renomear para histórico
   UPDATE cloud_account 
   SET state = 'DECOMMISSIONED', 
       name = CONCAT(name, '-DECOMMISSIONED-', CURRENT_DATE()) 
   WHERE id = 'ID_DA_CONTA_ANTIGA';
   ```

### Passo 1: Provisionamento das Novas Contas Root (Automatizado com Playwright)
Use o Playwright para criar e configurar a estrutura básica das contas:
1. **AWS Registration Automation:**
   O script acessa o console AWS, preenche os dados do proprietário `${AWS_ROOT_EMAIL}`, insere os dados de faturamento do `.env` (`BILLING_CARD_NUMBER`, etc.) e ativa o AWS Organizations:
   ```bash
   npx playwright test tests/dr-aws-registration.spec.ts
   ```
2. **GCP Account Setup Automation:**
   O script realiza login com a conta `${GCP_ROOT_EMAIL}`, cria a estrutura inicial de pastas e vincula a conta de faturamento correspondente:
   ```bash
   npx playwright test tests/dr-gcp-setup.spec.ts
   ```

### Passo 2: Configuração de Acessos de API (Automatizado com Playwright)
1. **AWS API Bootstrap:**
   O script cria a IAM Role administrativa necessária e gera chaves de acesso temporárias, configurando-as no `.env` do Cloud Manager:
   ```bash
   npx playwright test tests/dr-aws-bootstrap-iam.spec.ts
   ```
2. **GCP API Bootstrap:**
   O script cria o projeto administrativo principal (ex: `gcp-admin-core`), ativa as APIs de Resource Manager e Billing, cria a Service Account com privilégios de Admin da Organização e exporta a chave JSON para o arquivo de ambiente:
   ```bash
   npx playwright test tests/dr-gcp-bootstrap-sa.spec.ts
   ```


### Passo 3: Atualização do Banco de Dados do Cloud Manager
As tabelas que armazenam as contas semente (seeds) devem ser atualizadas para refletir os novos IDs e e-mails.

```sql
-- Exemplo de atualização de Seeds na base de dados (substituir pelos e-mails reais contidos no .env)
UPDATE cloud_account 
SET email = 'SUA_NOVA_CONTA_AWS@domain.com', -- ler de ${AWS_ROOT_EMAIL}
    state = 'ACTIVE',
    updated_at = NOW()
WHERE provider = 'AWS' AND name = 'AWS-Master-Seed';

UPDATE cloud_account 
SET email = 'SUA_NOVA_CONTA_GCP@domain.com', -- ler de ${GCP_ROOT_EMAIL}
    state = 'ACTIVE',
    updated_at = NOW()
WHERE provider = 'GCP' AND name = 'GCP-Master-Seed';
```

### Passo 4: Validação de Conectividade (Smoke Tests)
Execute os scripts de validação de conectividade locais ou na pipeline para testar as novas chaves:
```bash
# Smoke test AWS
aws sts get-caller-identity

# Smoke test GCP
gcloud auth activate-service-account --key-file=path-to-new-key.json
gcloud projects list
```

### Passo 5: Re-população do Pool de Contas (Ready to Book)
Como a conta base mudou, as contas antigas do pool devem ser invalidadas ou migradas manualmente, e novas contas do pool devem ser geradas a partir do novo fluxo automatizado.
1. Marque as contas do pool associadas ao seed antigo como `DECOMMISSIONED` ou `ERROR`.
2. Acione o endpoint ou envie mensagem SQS para pré-provisionar novas contas na nova estrutura master.
