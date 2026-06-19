# Portfolio API

Sistema de gerenciamento do portfolio de projetos de uma empresa: ciclo de vida
completo, gerenciamento de equipe, orcamento e classificacao de risco dinamica.

## Stack

- Java 17 + Spring Boot 3.3
- Spring Data JPA + Hibernate
- PostgreSQL + Flyway (migrations)
- Spring Security + JWT (access token + refresh token com rotacao automatica)
- Springdoc OpenAPI (Swagger UI)
- JUnit 5 + Mockito + AssertJ + JaCoCo
- Docker + Docker Compose

---

## Como rodar

### Opcao 1: Docker Compose (recomendado)

```bash
docker compose up --build
```

Isso sobe PostgreSQL e a aplicacao automaticamente. Acesse:

- API: http://localhost:8080
- Swagger: http://localhost:8080/swagger-ui.html

### Opcao 2: Local (sem Docker)

#### Pre-requisitos

- JDK 17 ou 21
- PostgreSQL rodando localmente
- Maven 3.8+

#### 1. Criar o banco

```sql
CREATE DATABASE portfolio_db;
```

#### 2. Configurar credenciais

Ajuste em `portfolio/src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/portfolio_db
spring.datasource.username=postgres
spring.datasource.password=postgres
```

#### 3. Executar

```bash
cd portfolio
mvn clean package -DskipTests
mvn spring-boot:run
```

---

## Usuarios padrao

Na primeira inicializacao, os usuarios padrao sao criados automaticamente:

| Usuario | Senha    | Roles       |
|---------|----------|-------------|
| admin   | admin123 | ADMIN, USER |
| user    | user123  | USER        |

---

## Autenticacao JWT

A API usa **JWT Bearer Token**. O fluxo completo:

### Login

```http
POST /auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}
```

Resposta:

```json
{
  "accessToken": "eyJhbGci...",
  "refreshToken": "550e8400-e29b-...",
  "tokenType": "Bearer",
  "expiresIn": 900000
}
```

### Usar o token nas requisicoes

```http
Authorization: Bearer eyJhbGci...
```

### Renovar o access token

```http
POST /auth/refresh
Content-Type: application/json

{
  "refreshToken": "550e8400-e29b-..."
}
```

### Logout

```http
POST /auth/logout
Content-Type: application/json

{
  "refreshToken": "550e8400-e29b-..."
}
```

### Registrar novo usuario (requer ADMIN)

```http
POST /auth/register
Content-Type: application/json

{
  "username": "novo",
  "password": "senha123",
  "roles": ["USER"]
}
```

### Permissoes por role

| Operacao                   | USER | ADMIN |
|----------------------------|------|-------|
| GET em qualquer /api/**    | Sim  | Sim   |
| POST, PUT, PATCH, DELETE   | Nao  | Sim   |
| /auth/** e /external/**    | Sim  | Sim   |

---

## Swagger UI

Acesse: http://localhost:8080/swagger-ui.html

Para autenticar no Swagger:

1. Faca `POST /auth/login`
2. Copie o `accessToken`
3. Clique em **Authorize** (cadeado) e cole o token

---

## Endpoints

### Autenticacao (`/auth`)

| Metodo | Endpoint        | Descricao                              | Auth   |
|--------|-----------------|----------------------------------------|--------|
| POST   | /auth/login     | Login (retorna access + refresh token) | Nao    |
| POST   | /auth/refresh   | Renova o access token                  | Nao    |
| POST   | /auth/logout    | Invalida o refresh token               | Nao    |
| POST   | /auth/register  | Cadastra novo usuario                  | ADMIN  |

### Projetos (`/api/projetos`)

| Metodo | Endpoint                    | Descricao                                         | Auth  |
|--------|-----------------------------|---------------------------------------------------|-------|
| GET    | /api/projetos               | Lista paginada com filtros                        | USER  |
| GET    | /api/projetos/{id}          | Busca por id (inclui classificacao de risco)      | USER  |
| POST   | /api/projetos               | Cria projeto (status inicial: EM_ANALISE)         | ADMIN |
| PUT    | /api/projetos/{id}          | Atualiza dados do projeto                         | ADMIN |
| PATCH  | /api/projetos/{id}/status   | Atualiza status respeitando sequencia logica      | ADMIN |
| DELETE | /api/projetos/{id}          | Remove projeto (bloqueado em certos status)       | ADMIN |

### Membros (`/api/membros`)

| Metodo | Endpoint          | Descricao                              | Auth  |
|--------|-----------------  |----------------------------------------|-------|
| GET    | /api/membros      | Lista membros via API externa mockada  | USER  |
| GET    | /api/membros/{id} | Busca membro por id                    | USER  |
| POST   | /api/membros      | Cria membro na API externa             | ADMIN |

### Relatorios (`/api/relatorios`)

| Metodo | Endpoint                  | Descricao                       | Auth |
|--------|---------------------------|---------------------------------|------|
| GET    | /api/relatorios/portfolio | Relatorio resumido do portfolio | USER |

### API Externa Mockada (`/external`)

| Metodo | Endpoint               | Descricao                     | Auth |
|--------|------------------------|-------------------------------|------|
| GET    | /external/membros      | Lista membros (mock externo)  | Nao  |
| GET    | /external/membros/{id} | Busca membro por id           | Nao  |
| POST   | /external/membros      | Cria membro no mock externo   | Nao  |

---

## Regras de Negocio

### Ciclo de vida do projeto (status)

Sequencia obrigatoria (nao e permitido pular etapas):

```
EM_ANALISE -> ANALISE_REALIZADA -> ANALISE_APROVADA -> INICIADO -> PLANEJADO -> EM_ANDAMENTO -> ENCERRADO
```

`CANCELADO` pode ser aplicado a partir de qualquer status, exceto `ENCERRADO` ou `CANCELADO`.

### Exclusao de projetos

Projetos com status `INICIADO`, `EM_ANDAMENTO` ou `ENCERRADO` **nao podem ser excluidos**.

### Classificacao de risco (calculada dinamicamente)

| Risco | Orcamento               | Prazo            |
|-------|-------------------------|------------------|
| BAIXO | ate R$ 100.000          | ate 3 meses      |
| MEDIO | R$ 100.001 a R$ 500.000 | 3 a 6 meses     |
| ALTO  | acima de R$ 500.000     | acima de 6 meses |

### Membros

- Apenas membros com atribuicao `FUNCIONARIO` podem ser associados a projetos
- Cada projeto deve ter entre **1 e 10 membros**
- Um membro nao pode estar em mais de **3 projetos ativos** simultaneamente (status diferente de `ENCERRADO` ou `CANCELADO`)

### Tokens JWT

- **Access token**: expira em 15 minutos (configuravel)
- **Refresh token**: expira em 7 dias (configuravel)
- **Rotacao automatica**: ao usar o refresh token, o anterior e revogado e um novo e emitido
- **Limpeza automatica**: tokens expirados/revogados sao removidos do banco diariamente a meia-noite

---

## Relatorio do Portfolio

`GET /api/relatorios/portfolio` retorna:

```json
{
  "quantidadeProjetosPorStatus": {
    "EM_ANALISE": 2,
    "EM_ANDAMENTO": 1,
    "ENCERRADO": 3
  },
  "totalOrcadoPorStatus": {
    "EM_ANALISE": 150000.00,
    "EM_ANDAMENTO": 500000.00,
    "ENCERRADO": 1200000.00
  },
  "mediaDuracaoProjetosEncerradosEmDias": 120.5,
  "totalMembrosUnicosAlocados": 8
}
```

---

## Testes

```bash
cd portfolio
mvn clean test
```

### Cobertura (JaCoCo)

```bash
cd portfolio
mvn clean verify
```

O relatorio HTML fica em: `portfolio/target/site/jacoco/index.html`

Meta minima configurada: **70% de cobertura de linha** nas classes de regras de negocio (`service` e `policy`).

---

## Arquitetura do projeto

```
portfolio/src/main/java/com/codegroup/portfolio/
  client/          # cliente HTTP para API externa
  config/          # configuracoes (Security, OpenAPI, Auth, RestTemplate)
  controller/      # endpoints REST
  dto/             # DTOs de request/response
  entity/          # entidades JPA
  enums/           # enumeracoes (Status, Risco, Atribuicao)
  exception/       # tratamento global de erros
  filter/          # filtro JWT
  mapper/          # conversao entity <-> DTO
  policy/          # regras de negocio extraidas (status, exclusao, alocacao)
  repository/      # repositorios JPA
  service/         # servicos de orquestracao
```

---

## Docker

### Subir o ambiente completo

```bash
docker compose up --build
```

### Parar

```bash
docker compose down
```

### Resetar banco

```bash
docker compose down -v
docker compose up --build
```
