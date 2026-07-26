# ADR 001: Adoção de Arquitetura Hexagonal com Domínio em Java Puro

**Status:** APROVADO
**Data:** 2026-07-25
**Autor:** Arquitetura de Software

---

## Contexto
Necessitamos isolar as regras de negócio de governança de nuvem da volatilidade de SDKs externos (AWS SDK, GCP SDK) e frameworks (Spring Boot, Hibernate).

## Decisão
O núcleo de domínio (`business`) será escrito estritamente em Java 21 puro sem anotações de terceiros. Toda injeção de dependências dos Casos de Uso será feita explicitamente via Java Config (`UseCaseConfig.java`).

## Consequências

### Positivas
* Testabilidade total sem Spring Context.
* Alta manutenibilidade e flexibilidade.
* Substituição simples de integradores de nuvem.

### Negativas
* Necessidade de mapeamento bidirecional de dados entre entidades de banco (`JpaEntity`) e domínio (`Entity`).
