-- Schema atual, do jeito que o ddl-auto:update do Hibernate ja tinha criado.
-- IF NOT EXISTS pra ser seguro tanto num banco novo (cria do zero) quanto num
-- banco existente (account/user_profile ja la, migration so registra o baseline).
CREATE TABLE IF NOT EXISTS account (
    username      VARCHAR(255) PRIMARY KEY,
    password_hash VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS user_profile (
    username    VARCHAR(255) PRIMARY KEY,
    description VARCHAR(2000)
);
