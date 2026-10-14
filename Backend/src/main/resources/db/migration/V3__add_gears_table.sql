CREATE TABLE gears (
    id                       UUID           PRIMARY KEY,
    owner_id                 UUID           NOT NULL,
    category                 VARCHAR(30)    NOT NULL,
    name                     VARCHAR(100)   NOT NULL,
    brand                    VARCHAR(100),
    model                    VARCHAR(100),
    serial_number            VARCHAR(100),
    purchase_date            DATE,
    last_service_date        DATE,
    service_interval_months  INTEGER,
    notes                    VARCHAR(1000),
    attributes               JSONB          NOT NULL DEFAULT '{}'::jsonb,
    archived_at              TIMESTAMPTZ,
    created_at               TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ    NOT NULL DEFAULT now(),

    CONSTRAINT fk_gear_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_gear_category CHECK (category IN (
        'BCD', 'REGULATOR', 'OCTOPUS', 'COMPUTER', 'WETSUIT', 'SEMIDRYSUIT', 'DRYSUIT',
        'GLOVES', 'BOOTS', 'FINS', 'MASK', 'WEIGHTS',
        'TORCH', 'SMB', 'KNIFE', 'CAMERA', 'OTHER'
    )),
    CONSTRAINT ck_gear_service_interval CHECK (service_interval_months IS NULL OR service_interval_months > 0),
    CONSTRAINT ck_gear_attributes_object CHECK (jsonb_typeof(attributes) = 'object')
);

CREATE INDEX idx_gear_owner_category ON gears (owner_id, category);
CREATE INDEX idx_gear_owner_active ON gears (owner_id) WHERE archived_at IS NULL;