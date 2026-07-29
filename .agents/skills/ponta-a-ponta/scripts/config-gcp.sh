#!/bin/bash
# Script para Configuração e Autenticação Automatizada - GCP

echo "====================================================="
echo "   Configuração e Autenticação Automatizada - GCP"
echo "====================================================="
echo ""

# 1. Verificar dependências
echo "[INFO] Verificando dependências necessárias..."

if command -v gcloud &> /dev/null; then
  echo "[OK] Google Cloud CLI (gcloud) já está instalado: $(gcloud --version | head -n 1)"
else
  echo "[ERR] Google Cloud CLI (gcloud) não está instalado. Instale o gcloud CLI antes de prosseguir."
  exit 1
fi
echo ""

# 2. Detectar sessão anterior
GCP_PROFILE_ENV_FILE=".gcp_profile_env"
PREVIOUS_PROJECT=""

if [ -f "$GCP_PROFILE_ENV_FILE" ]; then
  PREVIOUS_PROJECT=$(grep "export GCP_PROJECT=" "$GCP_PROFILE_ENV_FILE" | cut -d'=' -f2 | tr -d '"')
  if [ -n "$PREVIOUS_PROJECT" ]; then
    echo "[INFO] Detectado projeto anterior no arquivo de ambiente: $PREVIOUS_PROJECT"
  fi
fi

# Detectar conta ativa atual
GCP_ACCOUNT=$(gcloud config get-value account 2>/dev/null)
SESSION_VALID=false

if [ -n "$GCP_ACCOUNT" ] && [ "$GCP_ACCOUNT" != "(unset)" ]; then
  SESSION_VALID=true
  echo "[OK] Sessão ativa detectada para a conta GCP: $GCP_ACCOUNT"
else
  echo "[WARN] Nenhuma sessão ativa ativa detectada no gcloud."
fi

# Menu de Opções
echo "====================================================="
echo "               Autenticação GCP"
echo "====================================================="
echo ""
echo "Escolha uma opção:"
echo "1) Continuar com a sessão atual (Recomendado)"
echo "2) Realizar novo login (gcloud auth login)"
echo "3) Listar e selecionar um projeto GCP ativo"
echo "4) Configurar nova credencial/conta"

read -p "Opção [1-4] (padrão: 1): " OPTION
OPTION=${OPTION:-1}

case "$OPTION" in
  1)
    if [ "$SESSION_VALID" = false ]; then
      echo "[INFO] Iniciando login no gcloud..."
      gcloud auth login
    else
      echo "[INFO] Mantendo conta ativa atual."
    fi
    ;;
  2)
    echo "[INFO] Solicitando novo login no gcloud..."
    gcloud auth login
    ;;
  3)
    echo "[INFO] Buscando projetos disponíveis no GCP..."
    gcloud projects list
    ;;
  4)
    echo "[INFO] Inicializando fluxo de configuração gcloud..."
    gcloud init
    ;;
  *)
    echo "[ERR] Opção inválida."
    exit 1
    ;;
esac

# Recarrega a conta ativa pós-opção
GCP_ACCOUNT=$(gcloud config get-value account 2>/dev/null)
CURRENT_PROJECT=$(gcloud config get-value project 2>/dev/null)

if [ "$CURRENT_PROJECT" = "(unset)" ] || [ -z "$CURRENT_PROJECT" ]; then
  CURRENT_PROJECT="$PREVIOUS_PROJECT"
fi

echo ""
read -p "Qual ID do Projeto GCP deseja utilizar? [$CURRENT_PROJECT]: " PROJECT_ID
PROJECT_ID=${PROJECT_ID:-$CURRENT_PROJECT}

if [ -n "$PROJECT_ID" ] && [ "$PROJECT_ID" != "(unset)" ]; then
  gcloud config set project "$PROJECT_ID" 2>/dev/null
  echo "[OK] Projeto atualizado para: $PROJECT_ID"
fi

# Salvar arquivo de ambiente local
echo "[INFO] Salvando as configurações em $GCP_PROFILE_ENV_FILE..."
cat <<EOF > "$GCP_PROFILE_ENV_FILE"
export GCP_ACCOUNT="$GCP_ACCOUNT"
export GCP_PROJECT="$PROJECT_ID"
EOF

echo "[OK] Conta e Projeto salvos com sucesso em $GCP_PROFILE_ENV_FILE!"
