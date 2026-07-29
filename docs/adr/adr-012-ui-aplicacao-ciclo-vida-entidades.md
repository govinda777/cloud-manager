# ADR 012: Implementação da Interface Gráfica (UI) e Execução Local

**Status:** APROVADO
**Data:** 2026-07-29
**Autor:** Arquitetura e Engenharia de Software

---

## Contexto

A aplicação **Cloud Manager** precisa gerenciar de forma transparente o ciclo de vida completo de contas de nuvem (`CloudAccount`), que passam pelos estados `CREATED`, `IN_PROVISIONING`, `BILLING_LINKED`, `ACTIVE` e `FAILED`. 

Para que desenvolvedores, administradores e operadores possam interagir com esse fluxo sem a necessidade de ferramentas de linha de comando ou chamadas manuais de API (como `curl` ou Postman), torna-se imperativo implementar uma Interface Gráfica (UI) unificada. Essa interface deve funcionar localmente de forma simples, integrada ao ecossistema existente, e cobrir todos os estágios do ciclo de vida das entidades, bem como o dashboard gerencial.

## Decisão

Decidimos adotar a seguinte abordagem arquitetural e operacional para a implementação e execução da interface gráfica:

### 1. Stack Tecnológica e Arquitetura do Frontend
* **SPA Estática (Single Page Application):** Construção baseada em HTML5 Semântico, Vanilla CSS3 (com suporte a CSS Variables, temas modernos e transições fluidas) e Vanilla JavaScript (ES6+).
* **Distribuição Simplificada (BFF Embutido):** A UI será servida diretamente pelo servidor Spring Boot através do diretório `src/main/resources/static`. Isso elimina a complexidade de gerenciar múltiplos servidores locais, problemas de CORS em desenvolvimento e simplifica o empacotamento em contêineres Docker.
* **Acessibilidade:** A UI estará acessível na raiz do servidor local (ex: `http://localhost:8080/index.html` ou redirecionamento direto de `http://localhost:8080/`).

### 2. Ciclo de Vida Completo das Entidades na UI
A UI cobrirá 100% das etapas do ciclo de vida de `CloudAccount`:
1. **Formulário de Criação (State: CREATED):**
   * Entrada de dados estruturada: Nome da Conta, E-mail do Administrador, Provedor (`AWS` ou `GCP`), Centro de Custo, e seleção opcional de Conta Semente (`SeedAccountId`).
   * Validações de campos em tempo real no cliente antes do envio via `POST /accounts`.
2. **Acompanhamento de Provisionamento (State: IN_PROVISIONING & BILLING_LINKED):**
   * Listagem dinâmica das contas ativas e em progresso com indicadores visuais de progresso (ex: spinners, barras de progresso).
   * Atualização periódica (pooling suave ou atualização manual simplificada) para refletir a transição automática de estados no backend.
3. **Ativação e Sucesso (State: ACTIVE):**
   * Destaque visual em verde ou variantes elegantes para contas totalmente provisionadas e funcionais.
   * Exibição de metadados completos e do centro de custo associado.
4. **Tratamento de Falhas (State: FAILED):**
   * Identificação de contas com problemas de provisionamento através de alertas vermelhos intuitivos.
   * Exibição amigável do campo `errorMessage` retornado pelo backend para facilitar o diagnóstico rápido (Shift-Left) pelo operador.

### 3. Jornadas do Usuário (User Journeys)
Para garantir que a UI cubra as necessidades operacionais de forma intuitiva, mapeamos as seguintes jornadas principais:

* **Jornada 1: Provisionamento de Nova Conta (Caminho Feliz)**
  * **Ação:** O operador acessa a interface local, abre o formulário "Nova Conta", preenche os campos requeridos (Nome, E-mail, Provedor, Centro de Custo) e submete.
  * **Comportamento & Transição na UI:** A UI envia os dados ao backend (`POST /accounts`), o qual cria a conta com status `CREATED` e publica a mensagem de provisionamento. Na UI, um novo card surge instantaneamente sob a seção *"Em Processamento"* com um indicador visual de progresso (ex: animação de carregamento). Conforme o backend evolui o estado para `IN_PROVISIONING` e `BILLING_LINKED`, a UI atualiza o status dinamicamente. Quando a ativação conclui, o card é movido para a seção *"Contas Ativas"* com destaque visual positivo (verde).
* **Jornada 2: Detecção e Diagnóstico de Falha de Provisionamento**
  * **Ação:** O operador tenta criar uma conta, porém ocorre uma falha na comunicação ou validação com o provedor (AWS/GCP).
  * **Comportamento & Transição na UI:** A conta transiciona para `FAILED` no backend. Na UI, o card da conta é movido para a seção *"Falhas de Provisionamento"*, adquirindo destaque visual de alerta (vermelho). Um box ou tooltip de detalhes é exibido, extraindo e apresentando o conteúdo exato do campo `errorMessage` retornado pela API para que o operador saiba exatamente a causa raiz (ex: chaves expiradas ou falta de cotas) sem precisar inspecionar logs do servidor.
* **Jornada 3: Monitoramento FinOps e Governança**
  * **Ação:** O analista de custos ou gestor abre a página principal para auditar a distribuição de contas e recursos.
  * **Comportamento & Transição na UI:** A página consome o endpoint `/accounts/dashboard` e exibe cartões com o totalizador de status e gráficos estruturados de pizza/barra (ou listagens ordenadas) mostrando a distribuição percentual de contas por **Centro de Custo** (garantindo que não existam contas sem centro de custo atribuído) e por **Provedor Cloud** (AWS vs GCP).

### 4. Dashboard Consolidado
Integração de uma seção de visão geral alimentada pelo endpoint `/accounts/dashboard`, apresentando:
* Contadores de saúde (Total de contas, Contas em Criação, Ativas e Falhas).
* Distribuição de contas agregadas por Centro de Custo e Provedor através de elementos visuais limpos e fáceis de escanear.

### 5. Integração de Execução Local e Makefile
Para manter a consistência com a [ADR 011](file:///Users/govinda/projetos/cloud-manager/docs/adr/adr-011-configuracao-instalacao-local.md), a interface estará totalmente integrada ao workflow padrão:
* O comando `make up` iniciará a infraestrutura local e compilará/subirá o servidor backend contendo a UI embarcada.
* A documentação de instalação local e guias rápidos apontarão explicitamente para a URL local da UI.

### 6. Testes de Integração BDD para a UI e Ciclo de Vida
Para assegurar a robustez do fluxo consumido pela UI e evitar quebras de contrato das APIs, adotaremos a validação sistemática via Cucumber:
* **Mapeamento de Cenários BDD da UI:**
  * **Cenário de Consistência do Dashboard:** Validar que o endpoint `/accounts/dashboard` (que alimenta os cards gerenciais e os gráficos de setores da UI) atualiza seus contadores corretamente em tempo real após a criação, ativação ou falha de contas.
  * **Cenário de Propagação do Erro:** Validar que, quando uma conta cloud falha no provisionamento, o status `FAILED` e a descrição da falha (`errorMessage`) sejam expostos detalhadamente na resposta do endpoint de consulta de contas (permitindo que a UI exiba corretamente o diagnóstico de erro).
* **Automação no Pre-Push:** Estes testes integrados de BDD farão parte do escopo executado automaticamente via `make test-bdd` na fase de *pre-push* (conforme estabelecido na [ADR 011](file:///Users/govinda/projetos/cloud-manager/docs/adr/adr-011-configuracao-instalacao-local.md)), garantindo a integridade dos contratos das APIs antes de qualquer deploy.

---

## Consequências

### Positivas
* **Experiência de Desenvolvimento (DX):** Sem dependência de ferramentas pesadas como Node.js, Webpack ou NPM no ambiente local do desenvolvedor Java. Rodar `make up` ou `mvn spring-boot:run` é suficiente para servir o frontend e o backend simultaneamente.
* **Visibilidade do Ciclo de Vida:** Facilita a validação manual imediata de fluxos de provisionamento e cenários de erro durante o ciclo de desenvolvimento local.
* **Baixo Acoplamento de Deploy:** O frontend consome puramente a API REST exposta no mesmo host/porta, permitindo que, no futuro, seja facilmente desacoplado se necessário.

### Negativas
* **Limitações de Framework:** A escolha por Vanilla JS/CSS3 exige mais disciplina na organização do código do frontend para evitar arquivos excessivamente longos ou repetição de lógica de manipulação de DOM.
* **Necessidade de Build Maven:** Qualquer alteração no frontend requer um reload de recursos estáticos pelo Spring Boot (embora suportado por ferramentas de desenvolvimento de live-reload do Spring Boot DevTools).
