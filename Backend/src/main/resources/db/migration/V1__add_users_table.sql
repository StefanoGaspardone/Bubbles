CREATE TABLE users (
    id                    UUID          PRIMARY KEY,
    first_name            VARCHAR(50)   NOT NULL,
    last_name             VARCHAR(50)   NOT NULL,
    username              VARCHAR(50)   NOT NULL,
    email                 VARCHAR(255)  NOT NULL,
    password_hash         VARCHAR(255)  NOT NULL,
    account_status        VARCHAR(20)   NOT NULL DEFAULT 'PENDING_ACTIVATION',
    role                  VARCHAR(20)   NOT NULL DEFAULT 'USER',
    notifications_enabled BOOLEAN       NOT NULL DEFAULT TRUE,
    avatar_id             UUID,
    email_verified_at     TIMESTAMPTZ,
    last_login_at         TIMESTAMPTZ,
    created_at            TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT uq_users_username       UNIQUE (username),
    CONSTRAINT uq_users_email          UNIQUE (email),
    CONSTRAINT ck_users_account_status CHECK (account_status IN ('PENDING_ACTIVATION', 'ACTIVE', 'SUSPENDED', 'DELETED')),
    CONSTRAINT ck_users_role           CHECK (role IN ('USER', 'ADMIN'))
);