CREATE TABLE email_verifications (
    id            UUID          PRIMARY KEY,
    user_id       UUID          NOT NULL,
    otp_hash      VARCHAR(255)  NOT NULL,
    expires_at    TIMESTAMPTZ   NOT NULL,
    attempts      INTEGER       NOT NULL DEFAULT 0,
    last_sent_at  TIMESTAMPTZ   NOT NULL,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT uq_email_verifications_user UNIQUE (user_id),
    CONSTRAINT fk_email_verifications_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_email_verifications_attempts CHECK (attempts >= 0)
);

CREATE TABLE password_resets (
    id            UUID          PRIMARY KEY,
    user_id       UUID          NOT NULL,
    otp_hash      VARCHAR(255)  NOT NULL,
    expires_at    TIMESTAMPTZ   NOT NULL,
    attempts      INTEGER       NOT NULL DEFAULT 0,
    last_sent_at  TIMESTAMPTZ   NOT NULL,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT uq_password_resets_user UNIQUE (user_id),
    CONSTRAINT fk_password_resets_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_password_resets_attempts CHECK (attempts >= 0)
);

CREATE TABLE refresh_tokens (
    id          UUID          PRIMARY KEY,
    user_id     UUID          NOT NULL,
    token_hash  VARCHAR(255)  NOT NULL,
    family_id   UUID          NOT NULL,
    expires_at  TIMESTAMPTZ   NOT NULL,
    revoked_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT uq_refresh_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_family_id ON refresh_tokens (family_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens (expires_at);