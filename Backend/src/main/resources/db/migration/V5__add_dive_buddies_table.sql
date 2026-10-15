CREATE TABLE dive_buddies (
    id                   UUID          PRIMARY KEY,
    dive_id              UUID          NOT NULL,
    position             INTEGER       NOT NULL,
    buddy_user_id        UUID,
    first_name           VARCHAR(50)   NOT NULL,
    last_name            VARCHAR(50),
    role                 VARCHAR(20)   NOT NULL DEFAULT 'BUDDY',
    confirmation_status  VARCHAR(20)   NOT NULL DEFAULT 'NOT_REQUIRED',
    confirmed_at         TIMESTAMPTZ,
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT fk_dive_buddies_dive FOREIGN KEY (dive_id) REFERENCES dives (id) ON DELETE CASCADE,
    CONSTRAINT fk_dive_buddies_user FOREIGN KEY (buddy_user_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT ck_dive_buddies_position CHECK (position >= 0),
    CONSTRAINT ck_dive_buddies_role CHECK (role IN ('BUDDY', 'INSTRUCTOR', 'GUIDE', 'STUDENT')),
    CONSTRAINT ck_dive_buddies_status CHECK (confirmation_status IN ('NOT_REQUIRED', 'PENDING', 'CONFIRMED')),
    CONSTRAINT ck_dive_buddies_custom_status CHECK (
        buddy_user_id IS NOT NULL OR confirmation_status <> 'PENDING'
    ),
    CONSTRAINT ck_dive_buddies_confirmed_at CHECK (
        (confirmation_status = 'CONFIRMED') = (confirmed_at IS NOT NULL)
    )
);

CREATE UNIQUE INDEX uq_dive_buddies_dive_user
    ON dive_buddies (dive_id, buddy_user_id)
    WHERE buddy_user_id IS NOT NULL;

CREATE INDEX idx_dive_buddies_dive_id ON dive_buddies (dive_id);
CREATE INDEX idx_dive_buddies_buddy_user_id ON dive_buddies (buddy_user_id);