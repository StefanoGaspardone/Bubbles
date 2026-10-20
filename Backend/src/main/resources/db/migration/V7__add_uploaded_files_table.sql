CREATE TABLE uploaded_files (
    id             UUID          PRIMARY KEY,
    owner_user_id  UUID          NOT NULL,
    owner_type     VARCHAR(30)   NOT NULL,
    owner_id       UUID          NOT NULL,
    visibility     VARCHAR(10)   NOT NULL,
    file_name      VARCHAR(255)  NOT NULL,
    object_key     VARCHAR(512)  NOT NULL,
    content_type   VARCHAR(100)  NOT NULL,
    size           BIGINT        NOT NULL,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT fk_uploaded_files_owner_user FOREIGN KEY (owner_user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_uploaded_files_object_key UNIQUE (object_key),

    CONSTRAINT ck_uploaded_files_owner_type CHECK (owner_type IN (
        'USER_AVATAR', 'DIVE_PHOTO', 'CERTIFICATION', 'GEAR'
    )),
    CONSTRAINT ck_uploaded_files_visibility CHECK (visibility IN ('PUBLIC', 'PRIVATE')),
    CONSTRAINT ck_uploaded_files_key_prefix CHECK (
        (visibility = 'PUBLIC'  AND object_key LIKE 'public/%') OR
        (visibility = 'PRIVATE' AND object_key LIKE 'private/%')
    ),
    CONSTRAINT ck_uploaded_files_size CHECK (size > 0)
);

CREATE INDEX idx_uploaded_files_owner ON uploaded_files (owner_type, owner_id, created_at DESC);
CREATE INDEX idx_uploaded_files_owner_user_id ON uploaded_files (owner_user_id);

ALTER TABLE users
    ADD CONSTRAINT fk_users_avatar FOREIGN KEY (avatar_id)
        REFERENCES uploaded_files (id) ON DELETE SET NULL;

ALTER TABLE certifications
    ADD CONSTRAINT fk_certifications_front_image FOREIGN KEY (front_image_id)
        REFERENCES uploaded_files (id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_certifications_back_image FOREIGN KEY (back_image_id)
        REFERENCES uploaded_files (id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_certifications_document FOREIGN KEY (document_id)
        REFERENCES uploaded_files (id) ON DELETE SET NULL;