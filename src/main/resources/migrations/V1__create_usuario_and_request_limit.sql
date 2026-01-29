-- ==============================
-- TABELA USUARIO
-- ==============================
CREATE TABLE IF NOT EXISTS usuario (
                                       id BIGSERIAL PRIMARY KEY,
                                       email VARCHAR(255) NOT NULL UNIQUE,
    ultima_receita_criada varchar,
    numero_requisicoes INTEGER NOT NULL DEFAULT 0,
    data_ultima_requisicao TIMESTAMP,
    admin BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT    NOW()
    );


