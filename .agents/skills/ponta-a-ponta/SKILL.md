---
name: ponta-a-ponta
description: Guia de desenvolvimento e implementação ponta a ponta para o projeto Cloud Manager, organizando releases, entregáveis, casos de uso, testes (pre-commit/pre-push), configuração, relatórios, observabilidade (Uptime Robot, Dashboard de Negócio), contas e pipeline.
---

# Skill Ponta a Ponta - Cloud Manager

Esta skill orienta o desenvolvimento, teste e entrega contínua do projeto **Cloud Manager** de ponta a ponta. Siga rigorosamente esta estrutura para planejar releases, implementar casos de uso, configurar a observabilidade e validar a pipeline de CI/CD e conexões com as nuvens.

---

## 1. Ciclo de Releases e Entregáveis

### Releases
Toda nova feature ou alteração deve ser mapeada em uma release incremental (ex: `v1.0.0`, `v1.1.0`). Cada release deve conter:
- **Notas de Release (Changelog):** Descrição das melhorias, correções de bugs e migrações de banco de dados/infraestrutura necessárias.
- **Plano de Rollback:** Passos explícitos para reverter as alterações caso ocorram falhas em produção.

### Entregáveis (Deliverables)
Cada ciclo de entrega deve produzir:
1. **Artefatos de Infraestrutura:** Arquivos Terraform/CloudFormation validados.
2. **Código de Aplicação:** Container Docker publicado e taggeado de acordo com a release.
3. **Mapeamento de Custos:** Atualização do Billing Report associado às novas contas criadas.

---

## 2. Casos de Uso (Use Cases) e Casos de Teste (Test Cases)

### Casos de Uso Mapeados
1. **Provisionamento de Contas Multi-Cloud:** Criação automática de contas/projetos na AWS, GCP e Azure.
2. **Associação de Centro de Custo:** Vinculação obrigatória de cada nova conta a um centro de custo válido.
3. **Geração de Relatórios Financeiros:** Consolidação de consumo e projeção de gastos por centro de custo.

### Casos de Teste
Para cada caso de uso, deve haver:
- **Testes de Sucesso:** Fluxo feliz com todos os parâmetros válidos.
- **Testes de Exceção/Falha:** Comportamento do sistema quando APIs das clouds estão fora do ar, chaves de API expiradas, ou dados inválidos.

---

## 3. Configuração e Variáveis de Ambiente

As configurações devem ser centralizadas e injetadas via variáveis de ambiente usando arquivos `.env` ou gerenciadores de segredos (como AWS Secrets Manager ou GCP Secret Manager).

### Estrutura de Configurações
- **Ambientes:** Separação estrita entre `development`, `staging` e `production`.
- **Validação de Configurações:** A aplicação deve falhar na inicialização se variáveis obrigatórias de nuvem ou chaves privadas estiverem ausentes.

---

## 4. Observabilidade, Uptime Robot e Dashboards de Negócio

### Uptime Robot
Monitoramento de disponibilidade externa configurado para:
- Endpoints de saúde da API (`/health` ou `/status`).
- Tempo de resposta alvo: < 500ms.
- Alertas direcionados aos canais de comunicação do time de engenharia.

### Dashboard de Negócio
Painel executivo contendo métricas em tempo real sobre:
1. **Contas em Criação:** Quantidade de contas no pipeline de provisionamento, com indicação de gargalos ou falhas.
2. **Contas por Centro de Custo:** Distribuição percentual e absoluta de recursos e contas por departamento.
3. **Billing Report:** Visualização gráfica de custos atuais, agrupados por nuvem e centro de custo, destacando desvios do orçamento.

---

## 5. Estratégia de Testes Locais e Git Hooks

Seguimos a política estrita de automação de testes nas fases do Git:

### Pre-Commit (Testes Unitários)
- **O que rodar:** Apenas testes de unidade locais de execução rápida (sem chamadas externas a redes ou clouds).
- **Objetivo:** Garantir que nenhuma alteração quebre regras de negócio isoladas ou padrões de formatação.
- **Configuração sugerida:** Utilizar `husky` e `lint-staged` para rodar linters e os testes unitários da stack relevante.

### Pre-Push (Testes de Integração)
- **O que rodar:** Testes de integração que validam a comunicação entre componentes internos (ex: banco de dados, filas) e mocks locais das APIs de nuvem.
- **Objetivo:** Garantir estabilidade antes que o código seja enviado ao repositório remoto.

---

## 6. Validação Padrão de Pipeline e Acesso às Clouds

Para mitigar problemas de deploy, a pipeline e o acesso às nuvens devem ser testados usando um padrão claro:

### Teste de Pipeline Local
Antes de realizar o push, a pipeline de CI/CD (ex: GitHub Actions, GitLab CI) pode ser simulada usando ferramentas locais como `act` para rodar os workflows de integração contínua localmente:
```bash
# Rodar jobs da pipeline localmente usando act (se disponível)
act
```

### Teste de Conectividade com as Clouds
A aplicação deve conter scripts de validação rápida (Smoke Tests) para testar as credenciais e acessos antes do deploy:
- **AWS:** Executar `aws sts get-caller-identity` via SDK/CLI.
- **GCP:** Executar `gcloud auth list` ou chamada leve de API para validar chaves de serviço.
- **Azure:** Executar chamada leve para listar assinaturas ativas.

Esses scripts devem fazer parte do estágio de "Pre-flight check" da pipeline de CI.

---

## 7. Estratégia de CI/CD e Vending Machine de Contas (Master/Branchs)

Implementamos a promoção e criação isolada de contas em nuvem por meio do ciclo de vida das Branches/PRs integrado na pipeline de CI/CD:

### Contas Sandboxes/Filhas em Pull Requests (Branch-Environments)
- **Gatilho:** Abertura ou atualização de um Pull Request direcionado para as ramificações principais (`main` ou `master`).
- **Comportamento:** A pipeline de CI/CD provisiona de forma automatizada e isolada uma conta "filha" temporária (sandbox/PR-scoped) vinculada à respectiva conta Seed.
- **Utilidade:** Permite a execução dos testes BDD (Cucumber) e de regressão contra recursos cloud de verdade dentro de um escopo isolado antes de aprovar e realizar o merge do PR.

### Contas Master/Base em Merges (Staging/Production Promotion)
- **Gatilho:** Merge/Push bem sucedido diretamente nas branches `main` ou `master`.
- **Comportamento:** A pipeline atualiza ou promove a conta base "master" estável de infraestrutura e serviços compartilhados da aplicação.
- **Segurança:** Acesso e permissões a este estágio requerem chaves/roles restritas e assinadas digitalmente via OpenID Connect (OIDC).

