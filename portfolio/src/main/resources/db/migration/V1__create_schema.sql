-- V1: Criacao do schema inicial do portfolio

CREATE TABLE IF NOT EXISTS membro (
    id          BIGSERIAL PRIMARY KEY,
    nome        VARCHAR(255),
    atribuicao  VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS usuario (
    id       BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    ativo    BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS usuario_roles (
    usuario_id BIGINT      NOT NULL,
    role       VARCHAR(50) NOT NULL,
    PRIMARY KEY (usuario_id, role),
    CONSTRAINT fk_usuario_roles_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id)
);

CREATE TABLE IF NOT EXISTS projeto (
    id                BIGSERIAL       PRIMARY KEY,
    nome              VARCHAR(255)    NOT NULL,
    data_inicio       DATE            NOT NULL,
    previsao_termino  DATE            NOT NULL,
    data_real_termino DATE,
    orcamento_total   NUMERIC(15, 2)  NOT NULL,
    descricao         VARCHAR(2000),
    gerente_id        BIGINT,
    status            VARCHAR(50)     NOT NULL DEFAULT 'EM_ANALISE',
    CONSTRAINT fk_projeto_gerente FOREIGN KEY (gerente_id) REFERENCES membro(id)
);

CREATE TABLE IF NOT EXISTS projeto_membro (
    projeto_id BIGINT NOT NULL,
    membro_id  BIGINT NOT NULL,
    PRIMARY KEY (projeto_id, membro_id),
    CONSTRAINT fk_pm_projeto FOREIGN KEY (projeto_id) REFERENCES projeto(id),
    CONSTRAINT fk_pm_membro  FOREIGN KEY (membro_id)  REFERENCES membro(id)
);

CREATE TABLE IF NOT EXISTS refresh_token (
    id         BIGSERIAL    PRIMARY KEY,
    token      VARCHAR(255) NOT NULL UNIQUE,
    usuario_id BIGINT       NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revogado   BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_refresh_token_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id)
);
