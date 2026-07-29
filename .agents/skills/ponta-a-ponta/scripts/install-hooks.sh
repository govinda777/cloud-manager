#!/bin/bash
# Script para instalar os Git Hooks de pre-commit (testes unitários) e pre-push (testes de integração)

GIT_DIR=$(git rev-parse --git-dir 2>/dev/null)

if [ -z "$GIT_DIR" ]; then
  echo "Erro: Este diretório não é um repositório Git."
  exit 1
fi

PRE_COMMIT_HOOK="$GIT_DIR/hooks/pre-commit"
PRE_PUSH_HOOK="$GIT_DIR/hooks/pre-push"

echo "Instalando pre-commit hook..."
cat << 'EOF' > "$PRE_COMMIT_HOOK"
#!/bin/bash
echo "=== [Pre-commit] Executando testes unitários ==="
make test
if [ $? -ne 0 ]; then
  echo "=== [Pre-commit] FALHA: Testes unitários falharam! Commit abortado. ==="
  exit 1
fi
echo "=== [Pre-commit] SUCESSO: Todos os testes unitários passaram. ==="
exit 0
EOF

chmod +x "$PRE_COMMIT_HOOK"

echo "Instalando pre-push hook..."
cat << 'EOF' > "$PRE_PUSH_HOOK"
#!/bin/bash
echo "=== [Pre-push] Executando testes de integração ==="
make test-integration
if [ $? -ne 0 ]; then
  echo "=== [Pre-push] FALHA: Testes de integração falharam! Push abortado. ==="
  exit 1
fi
echo "=== [Pre-push] SUCESSO: Todos os testes de integração passaram. ==="
exit 0
EOF

chmod +x "$PRE_PUSH_HOOK"

echo "Hooks instalados com sucesso!"
echo "- pre-commit: roda 'make test'"
echo "- pre-push: roda 'make test-integration'"
