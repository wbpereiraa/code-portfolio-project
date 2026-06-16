# Portfolio API

Sistema de gerenciamento do portfólio de projetos de uma empresa: ciclo de vida,
equipe, orçamento e classificação de risco.

## Stack

- Java 17 + Spring Boot 3.3
- Spring Data JPA + Hibernate
- MySQL
- Spring Security (usuário/senha em memória)
- Springdoc OpenAPI (Swagger)
- JUnit 5 + Mockito + AssertJ + JaCoCo

## Como executar

1. Crie o banco MySQL:
```sql
CREATE DATABASE portfolio_db;
```

2. Ajuste `src/main/resources/application.properties` se necessário (usuário/senha do banco).

3. Rode a aplicação (`PortfolioApplication`). As tabelas são criadas automaticamente (`ddl-auto=update`).

## Autenticação (Basic Auth)

| Usuário | Senha    | Permissões                 |
|---------|----------|-----------------------------|
| admin   | admin123 | Leitura e escrita (todas as rotas) |
| user    | user123  | Apenas leitura (GET em /api/**)    |

Endpoints `/swagger-ui/**`, `/v3/api-docs/**` e `/external/**` são públicos.

## Documentação (Swagger)

http://localhost:8080/swagger-ui.html

## API externa mockada de membros

Para simular a integração com um sistema externo de RH, o cadastro de membros
é exposto em `/external/membros` (GET, GET/{id}, POST). O `MembroExternalClient`
consome esses endpoints como se fossem de outro serviço.

Já existem membros pré-cadastrados (ids 1 a 5), incluindo funcionários (FUNCIONARIO),
um gerente e um consultor.

## Endpoints principais

### Projetos (`/api/projetos`)
- `GET /api/projetos` — lista paginada com filtros: `nome`, `status`, `dataInicioDe`, `dataInicioAte`, `page`, `size`, `sort`
- `GET /api/projetos/{id}` — busca por id (inclui classificação de risco calculada)
- `POST /api/projetos` — cria projeto (status inicial sempre `EM_ANALISE`)
- `PUT /api/projetos/{id}` — atualiza dados do projeto
- `PATCH /api/projetos/{id}/status` — atualiza apenas o status, respeitando a sequência lógica
- `DELETE /api/projetos/{id}` — exclui (bloqueado se status `INICIADO`, `EM_ANDAMENTO` ou `ENCERRADO`)

### Membros (`/api/membros`)
- `GET /api/membros` — lista (delega para API externa mockada)
- `GET /api/membros/{id}` — busca por id
- `POST /api/membros` — cria membro na API externa (nome + atribuição/cargo)

### Relatórios (`/api/relatorios`)
- `GET /api/relatorios/portfolio` — relatório resumido:
  - Quantidade de projetos por status
  - Total orçado por status
  - Média de duração (dias) dos projetos encerrados
  - Total de membros únicos alocados

## Regras de negócio implementadas

- **Classificação de risco** calculada dinamicamente (orçamento x prazo em meses):
  BAIXO (≤ 100.000 e ≤ 3 meses), MEDIO, ALTO (> 500.000 ou > 6 meses).
- **Transição de status**: sequência fixa
  `EM_ANALISE → ANALISE_REALIZADA → ANALISE_APROVADA → INICIADO → PLANEJADO → EM_ANDAMENTO → ENCERRADO`,
  sem pular etapas. `CANCELADO` pode ser aplicado a qualquer momento, exceto a partir
  de `ENCERRADO` ou `CANCELADO`.
- **Exclusão bloqueada** para projetos com status `INICIADO`, `EM_ANDAMENTO` ou `ENCERRADO`.
- **Associação de membros**: apenas membros com atribuição `FUNCIONARIO` (validado via API
  externa); entre 1 e 10 membros por projeto; um membro não pode estar em mais de 3
  projetos com status diferente de `ENCERRADO`/`CANCELADO` simultaneamente.

## Testes

```bash
mvn test
```

Relatório de cobertura JaCoCo gerado em `target/site/jacoco/index.html`.
