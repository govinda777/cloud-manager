# ADR 011: Configuração e Instalação Local da Aplicação

**Status:** APROVADO
**Data:** 2026-07-28
**Autor:** Engenharia de Software

---

## Contexto
O projeto Cloud-Manager (CAPE) possui um ambiente de integração contínua rigoroso baseado em infraestrutura efêmera em cloud. Para evitar longos loops de feedback e garantir que a aplicação seja validada localmente da forma mais compatível possível com a cloud, precisamos de um mecanismo consistente para configuração, instalação, e testes na máquina do desenvolvedor. A infraestrutura local, composta por bancos de dados e simuladores de serviços cloud (como ElasticMQ para SQS), precisa ser orquestrada com facilidade, assim como a execução da suíte de testes.

## Decisão
Decidimos padronizar a configuração, instalação e ciclo de vida do ambiente local através de um `Makefile`.

O fluxo padrão usará os seguintes comandos para a infraestrutura e a aplicação localmente:
- `make setup`: Para inicializar o ambiente gerando configurações locais (ex: `.env`) e baixar as dependências em modo offline.
- `make up`: Para inicializar a infraestrutura local (PostgreSQL, ElasticMQ) via Docker Compose de forma transparente.
- `make test`: Para executar a bateria completa de testes unitários da aplicação.
- `make test-bdd`: Para executar testes de integração BDD (Cucumber), que validam os comportamentos chaves simulando interações com componentes reais.

Além disso, para alinhar a validação do código local com a validação do pipeline, decidimos implementar os seguintes controles no ciclo de vida de controle de versão (Git):
- **Pre-commit**: Executar os testes unitários (`make test`) em todos os commits, garantindo que o núcleo e componentes isolados não sejam quebrados.
- **Pre-push**: Executar os testes de integração BDD (`make test-bdd`) antes do envio do código remoto, garantindo que o comportamento sistêmico e as integrações mais pesadas funcionem antes que recursos de infraestrutura efêmera sejam invocados pela pipeline CI/CD de PRs.

## Consequências

### Positivas
- **Developer Experience (DX) Consistente**: Redução drástica na dificuldade de integração e configuração inicial; novos membros podem subir o ambiente com poucos comandos.
- **Detecção Precoce de Problemas (Shift-Left)**: Ao trazer validações profundas (BDD e Unitárias) para o fluxo de desenvolvimento no *pre-commit* e *pre-push*, evitamos loops caros na cloud.
- **Simetria Local/Cloud**: Reduz a distância de comportamento entre a máquina do desenvolvedor (onde serviços cloud são simulados por ex: ElasticMQ) e as implantações AWS/GCP reais.
- **Previsibilidade Operacional**: O `Makefile` funciona como uma documentação executável das operações corriqueiras.

### Negativas
- **Tempo de Execução Local**: O *pre-push* ao rodar cenários BDD pode prolongar o tempo necessário para submeter código (git push), impactando a experiência de desenvolvedores se a suíte de testes crescer muito e ficar lenta.
- **Manutenção de Dependências Locais**: Exigência de que desenvolvedores tenham o ecossistema base mínimo configurado localmente (Make, Docker Compose e ferramentas do SO) para o bom funcionamento do wrapper.
