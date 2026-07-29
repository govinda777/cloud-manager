#!/bin/bash
# Script padrão para testar o acesso com as Clouds (AWS, GCP, Azure) e testar a pipeline local

echo "=== Iniciando Verificações de Acesso às Clouds e Pipeline ==="
echo ""

# 1. AWS Access Check
echo "--- [AWS] Verificando acesso..."
if command -v aws &> /dev/null; then
  AWS_IDENTITY=$(aws sts get-caller-identity --query "Arn" --output text 2>&1)
  if [ $? -eq 0 ]; then
    echo "✅ Conexão AWS ativa: $AWS_IDENTITY"
  else
    echo "❌ Falha ao conectar à AWS. Detalhes: $AWS_IDENTITY"
  fi
else
  echo "⚠️  AWS CLI não instalada localmente."
fi
echo ""

# 2. GCP Access Check
echo "--- [GCP] Verificando acesso..."
if command -v gcloud &> /dev/null; then
  GCP_ACCOUNT=$(gcloud config get-value account 2>&1)
  if [ $? -eq 0 ] && [ "$GCP_ACCOUNT" != "(unset)" ]; then
    echo "✅ Conexão GCP ativa. Conta: $GCP_ACCOUNT"
  else
    echo "❌ Falha ao conectar ao GCP ou nenhuma conta ativa configurada."
  fi
else
  echo "⚠️  gcloud CLI não instalada localmente."
fi
echo ""

# 3. Azure Access Check
echo "--- [Azure] Verificando acesso..."
if command -v az &> /dev/null; then
  AZ_ACCOUNT=$(az account show --query "name" --output tsv 2>&1)
  if [ $? -eq 0 ]; then
    echo "✅ Conexão Azure ativa. Assinatura: $AZ_ACCOUNT"
  else
    echo "❌ Falha ao conectar à Azure ou nenhum login ativo."
  fi
else
  echo "⚠️  Azure CLI não instalada localmente."
fi
echo ""

# 4. Pipeline Validation Check
echo "--- [Pipeline] Verificando ambiente de CI local (act / Docker)..."
if command -v docker &> /dev/null; then
  if docker info &> /dev/null; then
    echo "✅ Docker Daemon em execução."
  else
    echo "❌ Docker Daemon não está em execução."
  fi
else
  echo "⚠️  Docker não está instalado."
fi

if command -v act &> /dev/null; then
  echo "✅ ferramenta 'act' instalada. Você pode rodar 'act' para testar os Workflows do GitHub Actions localmente."
else
  echo "ℹ️  'act' não instalado. Para validar pipelines localmente: brew install nektos/tap/act"
fi

echo ""
echo "=== Verificação Concluída ==="
