#!/bin/bash
# Script para Configuração e Autenticação Automatizada - AWS SSO

echo "====================================================="
echo "   Configuração e Autenticação Automatizada - AWS SSO"
echo "====================================================="
echo ""

# 1. Verificar dependências
echo "[INFO] Verificando dependências necessárias..."

if command -v git &> /dev/null; then
  echo "[OK] Git já está instalado: $(git --version)"
else
  echo "[ERR] Git não está instalado."
fi

if command -v gh &> /dev/null; then
  echo "[OK] GitHub CLI (gh) já está instalado: $(gh --version | head -n 1)"
else
  echo "[WARN] GitHub CLI (gh) não está instalado."
fi

if command -v aws &> /dev/null; then
  echo "[OK] AWS CLI já está instalado: $(aws --version | head -n 1)"
else
  echo "[ERR] AWS CLI não está instalado. Instale o AWS CLI antes de prosseguir."
  exit 1
fi
echo ""

# 2. Detectar perfil anterior
AWS_PROFILE_ENV_FILE=".aws_profile_env"
PREVIOUS_PROFILE=""
PREVIOUS_REGION="us-east-1"

if [ -f "$AWS_PROFILE_ENV_FILE" ]; then
  # Carrega variáveis antigas
  PREVIOUS_PROFILE=$(grep "export AWS_PROFILE=" "$AWS_PROFILE_ENV_FILE" | cut -d'=' -f2 | tr -d '"')
  PREVIOUS_REGION=$(grep "export AWS_REGION=" "$AWS_PROFILE_ENV_FILE" | cut -d'=' -f2 | tr -d '"')
  if [ -n "$PREVIOUS_PROFILE" ]; then
    echo "[INFO] Detectado perfil anterior no arquivo de ambiente: $PREVIOUS_PROFILE"
  fi
fi

# Tenta ler perfis do AWS CLI
PROFILES=$(aws configure list-profiles 2>/dev/null)

select_profile_and_auth() {
  local target_profile="$1"
  
  if [ -z "$target_profile" ]; then
    # Se não houver perfil anterior, listar ou solicitar um
    if [ -n "$PROFILES" ]; then
      echo "[INFO] Perfis AWS configurados disponíveis:"
      echo "$PROFILES"
      echo ""
      read -p "Digite o nome do perfil AWS SSO que deseja usar: " target_profile
    else
      echo "[WARN] Nenhum perfil AWS configurado encontrado."
      read -p "Digite o nome do perfil AWS SSO que deseja criar/usar: " target_profile
    fi
  fi

  if [ -z "$target_profile" ]; then
    echo "[ERR] Nenhum perfil foi selecionado."
    exit 1
  fi

  # Menu de Opções
  echo "====================================================="
  echo "               Autenticação AWS SSO"
  echo "====================================================="
  echo ""
  
  # Verificar se a sessão atual é válida
  SESSION_VALID=false
  echo "[INFO] Validando sessão ativa para o perfil '$target_profile'..."
  AWS_IDENTITY=$(aws sts get-caller-identity --profile "$target_profile" 2>&1)
  if [ $? -eq 0 ]; then
    SESSION_VALID=true
    echo "[OK] Sessão activa e válida detectada para o perfil: $target_profile"
  else
    echo "[WARN] Nenhuma sessão ativa válida para o perfil: $target_profile"
  fi

  echo "Escolha uma opção:"
  echo "1) Continuar com a sessão atual (Recomendado)"
  echo "2) Realizar novo login SSO neste perfil (aws sso login)"
  echo "3) Configurar uma nova sessão/perfil SSO (aws configure sso)"
  echo "4) Selecionar outro perfil SSO existente"
  
  read -p "Opção [1-4] (padrão: 1): " OPTION
  OPTION=${OPTION:-1}

  case "$OPTION" in
    1)
      if [ "$SESSION_VALID" = false ]; then
        echo "[INFO] Iniciando login SSO para o perfil '$target_profile'..."
        aws sso login --profile "$target_profile"
      else
        echo "[INFO] Mantendo sessão ativa atual."
      fi
      ;;
    2)
      echo "[INFO] Solicitando login SSO para o perfil '$target_profile'..."
      aws sso login --profile "$target_profile"
      ;;
    3)
      echo "[INFO] Iniciando configuração de novo perfil SSO..."
      aws configure sso
      # Após configurar, tenta ler o novo perfil criado
      read -p "Digite o nome do perfil que você acabou de criar para salvar nas configurações: " target_profile
      aws sso login --profile "$target_profile"
      ;;
    4)
      # Limpa o perfil alvo e chama recursivamente
      select_profile_and_auth ""
      return
      ;;
    *)
      echo "[ERR] Opção inválida."
      exit 1
      ;;
  esac

  # Validação final
  echo ""
  echo "[INFO] Validando sessão ativa para o perfil '$target_profile'..."
  AWS_IDENTITY=$(aws sts get-caller-identity --profile "$target_profile" 2>&1)
  if [ $? -eq 0 ]; then
    echo "[OK] Sessão AWS SSO validada com sucesso!"
    echo "  - Detalhes: $AWS_IDENTITY"
    
    # Salvar no arquivo
    read -p "Qual região da AWS deseja utilizar? [$PREVIOUS_REGION]: " REGION
    REGION=${REGION:-$PREVIOUS_REGION}

    echo "[INFO] Salvando as configurações em $AWS_PROFILE_ENV_FILE..."
    cat <<EOF > "$AWS_PROFILE_ENV_FILE"
export AWS_PROFILE="$target_profile"
export AWS_REGION="$REGION"
EOF
    echo "[OK] Perfil e Região salvos com sucesso em $AWS_PROFILE_ENV_FILE!"
  else
    echo "[ERR] Falha ao autenticar na AWS: $AWS_IDENTITY"
    exit 1
  fi
}

select_profile_and_auth "$PREVIOUS_PROFILE"
