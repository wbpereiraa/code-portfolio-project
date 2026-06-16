# Portfolio API

Sistema de gerenciamento do portfólio de projetos de uma empresa: ciclo de vida completo,
gerenciamento de equipe, orçamento e classificação de risco dinâmica.

## Stack

- Java 17 + Spring Boot 3.3
- Spring Data JPA + Hibernate
- PostgreSQL
- Spring Security + JWT (access token + refresh token com rotação automática)
- Springdoc OpenAPI (Swagger UI)
- JUnit 5 + Mockito + AssertJ + JaCoCo

---

## Pré-requisitos

- JDK 17 ou 21 (não use JDK 24+, incompatível com Lombok)
- PostgreSQL rodando localmente
- Maven 3.8+

---

## Configuração

### 1. Banco de dados

Crie o banco no PostgreSQL:

```sql
CREATE DATABASE portfolio_db;
```

### 2. application.properties

Ajuste as credenciais em `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/portfolio_db
spring.datasource.username=postgres
spring.datasource.password=postgres
```

As propriedades JWT já estão configuradas com valores padrão. Em produção, troque o `jwt.secret`
por uma string aleatória segura de no mínimo 32 caracteres:

```properties
jwt.secret=sua-chave-secreta-aqui-minimo-32-chars
jwt.access-token-expiration-ms=900000
jwt.refresh-token-expiration-ms=604800000
```

### 3. IntelliJ — configurações importantes

- `File > Project Structure > Project > SDK` → **JDK 17 ou 21**
- `Settings > Build > Maven > Runner > JRE` → mesmo JDK 17 ou 21
- `Settings > Build > Compiler > Annotation Processors` → **Enable annotation processing** marcado
- Plugin **Lombok** instalado e ativo

---

## Executando

```bash
mvn clean package
mvn spring-boot:run
```

Na primeira inicialização, os usuários padrão são criados automaticamente:

| Usuário | Senha    | Roles       |
|---------|----------|-------------|
| admin   | admin123 | ADMIN, USER |
| user    | user123  | USER        |

---

## Autenticação JWT

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

### Usar o token nas requisições
```http
Authorization: Bearer eyJhbGci...
```

### Renovar o access token (refresh)
```http
POST /auth/refresh
Content-Type: application/json

{
  "refreshToken": "550e8400-e29b-..."
}
```
Retorna um novo par `accessToken` + `refreshToken` (rotação automática — o token anterior é invalidado).

### Logout
```http
POST /auth/logout
Content-Type: application/json

{
  "refreshToken": "550e8400-e29b-..."
}
```

### Registrar novo usuário (requer role ADMIN)
```http
POST /auth/register
Content-Type: application/json

{
  "username": "novo",
  "password": "senha123",
  "roles": ["USER"]
}
```

### Permissões por role

| Operação                   | USER | ADMIN |
|----------------------------|------|-------|
| GET em qualquer /api/**    | ✅   | ✅    |
| POST, PUT, PATCH, DELETE   | ❌   | ✅    |
| /auth/** e /external/**    | ✅   | ✅    |

---

## Documentação (Swagger UI)

Acesse: http://localhost:8080/swagger-ui.html

Para autenticar no Swagger:
1. Faça `POST /auth/login`
2. Copie o `accessToken`
3. Clique em **Authorize** (cadeado) e cole o token

---

## Endpoints

### Autenticação (`/auth`)

| Método | Endpoint        | Descrição                              | Auth   |
|--------|-----------------|----------------------------------------|--------|
| POST   | /auth/login     | Login — retorna access + refresh token | Não    |
| POST   | /auth/refresh   | Renova o access token                  | Não    |
| POST   | /auth/logout    | Invalida o refresh token               | Não    |
| POST   | /auth/register  | Cadastra novo usuário                  | ADMIN  |

### Projetos (`/api/projetos`)

| Método | Endpoint                    | Descrição                                         | Auth  |
|--------|-----------------------------|---------------------------------------------------|-------|
| GET    | /api/projetos               | Lista paginada com filtros                        | USER  |
| GET    | /api/projetos/{id}          | Busca por id (inclui classificação de risco)      | USER  |
| POST   | /api/projetos               | Cria projeto (status inicial: EM_ANALISE)         | ADMIN |
| PUT    | /api/projetos/{id}          | Atualiza dados do projeto                         | ADMIN |
| PATCH  | /api/projetos/{id}/status   | Atualiza status respeitando sequência lógica      | ADMIN |
| DELETE | /api/projetos/{id}          | Remove projeto (bloqueado em certos status)       | ADMIN |

**Filtros disponíveis no GET /api/projetos:**
```
?nome=sistema&status=EM_ANDAMENTO&dataInicioDe=2026-01-01&dataInicioAte=2026-12-31&page=0&size=10&sort=nome,asc
```

### Membros (`/api/membros`)

| Método | Endpoint        | Descrição                              | Auth  |
|--------|-----------------|----------------------------------------|-------|
| GET    | /api/membros    | Lista membros via API externa mockada  | USER  |
| GET    | /api/membros/{id} | Busca membro por id                  | USER  |
| POST   | /api/membros    | Cria membro na API externa             | ADMIN |

### Relatórios (`/api/relatorios`)

| Método | Endpoint                  | Descrição                     | Auth |
|--------|---------------------------|-------------------------------|------|
| GET    | /api/relatorios/portfolio | Relatório resumido do portfólio | USER |

### API Externa Mockada (`/external`)

| Método | Endpoint             | Descrição                        | Auth |
|--------|----------------------|----------------------------------|------|
| GET    | /external/membros    | Lista membros (mock externo)     | Não  |
| GET    | /external/membros/{id} | Busca membro por id            | Não  |
| POST   | /external/membros    | Cria membro no mock externo      | Não  |

---

## Regras de Negócio

### Ciclo de vida do projeto (status)

Sequência obrigatória — não é permitido pular etapas:

```
EM_ANALISE → ANALISE_REALIZADA → ANALISE_APROVADA → INICIADO → PLANEJADO → EM_ANDAMENTO → ENCERRADO
```

`CANCELADO` pode ser aplicado a partir de qualquer status, exceto `ENCERRADO` ou `CANCELADO`.

### Exclusão de projetos

Projetos com status `INICIADO`, `EM_ANDAMENTO` ou `ENCERRADO` **não podem ser excluídos**.

### Classificação de risco (calculada dinamicamente)

| Risco | Orçamento              | Prazo         |
|-------|------------------------|---------------|
| BAIXO | até R$ 100.000         | até 3 meses   |
| MEDIO | R$ 100.001 a R$ 500.000| 3 a 6 meses   |
| ALTO  | acima de R$ 500.000    | acima de 6 meses |

### Membros

- Apenas membros com atribuição `FUNCIONARIO` podem ser associados a projetos
- Cada projeto deve ter entre **1 e 10 membros**
- Um membro não pode estar em mais de **3 projetos ativos** simultaneamente (status diferente de `ENCERRADO` ou `CANCELADO`)

### Tokens JWT

- **Access token**: expira em 15 minutos (configurável via `jwt.access-token-expiration-ms`)
- **Refresh token**: expira em 7 dias (configurável via `jwt.refresh-token-expiration-ms`)
- **Rotação automática**: ao usar o refresh token, o anterior é revogado e um novo é emitido
- **Limpeza automática**: tokens expirados/revogados são removidos do banco diariamente à meia-noite

---

## Relatório do Portfólio

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
  "mediaDuracaoProjetosEncerradosEmDias": 127.5,
  "totalMembrosUnicosAlocados": 8
}
```

---

## Testes

```bash
mvn test
```

Relatório de cobertura JaCoCo gerado em `target/site/jacoco/index.html`.

Cobertura implementada nas principais regras de negócio:
- `StatusProjetoTest` — valida todas as transições de status permitidas e bloqueadas
- `ClassificacaoRiscoServiceTest` — valida os três níveis de risco com limites exatos
- `ProjetoServiceTest` — valida criação, exclusão, transição de status, limite de membros, validação de datas
- `JwtServiceTest` — valida geração, validação e expiração de tokens
- `RefreshTokenServiceTest` — valida criação, revogação e expiração de refresh tokens

---

## Estrutura do Projeto

```
src/main/java/com/codegroup/portfolio/
├── auth/
│   ├── dto/          # LoginRequestDTO, TokenResponseDTO, RefreshRequestDTO, RegisterRequestDTO
│   ├── entity/       # Usuario, RefreshToken
│   ├── filter/       # JwtAuthenticationFilter
│   ├── repository/   # UsuarioRepository, RefreshTokenRepository
│   ├── service/      # AuthService, JwtService, RefreshTokenService, CustomUserDetailsService
│   └── AuthController.java
├── config/           # SecurityConfig, AuthConfig, OpenApiConfig, RestTemplateConfig, DataInitializer
├── controller/       # ProjetoController, MembroController, RelatorioController, MembroExternalApiMockController
├── client/           # MembroExternalClient
├── dto/              # ProjetoRequestDTO, ProjetoResponseDTO, MembroDTO, AtualizarStatusDTO, RelatorioPortfolioDTO
├── entity/           # Projeto, Membro
├── enums/            # StatusProjeto, ClassificacaoRisco, AtribuicaoMembro
├── exception/        # GlobalExceptionHandler, BusinessException, ResourceNotFoundException, ApiError
├── mapper/           # ProjetoMapper, MembroMapper
├── repository/       # ProjetoRepository, MembroRepository
└── service/          # ProjetoService, MembroService, RelatorioService, ClassificacaoRiscoService, ProjetoSpecification
```
