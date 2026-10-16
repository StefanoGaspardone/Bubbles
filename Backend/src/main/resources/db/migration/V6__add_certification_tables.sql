CREATE TABLE certifications (
    id                    UUID           PRIMARY KEY,
    owner_id              UUID           NOT NULL,
    agency                VARCHAR(20)    NOT NULL,
    agency_name           VARCHAR(100),
    certification_type    VARCHAR(20)    NOT NULL,
    course_name           VARCHAR(150)   NOT NULL,
    certification_number  VARCHAR(100),
    issue_date            DATE           NOT NULL,
    expiry_date           DATE,
    instructor_name       VARCHAR(150),
    instructor_number     VARCHAR(50),
    dive_center           VARCHAR(150),
    notes                 VARCHAR(1000),
    front_image_id        UUID,
    back_image_id         UUID,
    document_id           UUID,
    created_at            TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ    NOT NULL DEFAULT now(),

    CONSTRAINT fk_certifications_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_certifications_agency CHECK (agency IN (
        'PADI', 'SSI', 'CMAS', 'NAUI', 'SDI', 'TDI', 'RAID', 'GUE',
        'BSAC', 'IANTD', 'DAN', 'FIPSAS', 'OTHER'
    )),
    CONSTRAINT ck_certifications_agency_name CHECK (
        (agency = 'OTHER') = (agency_name IS NOT NULL)
    ),
    CONSTRAINT ck_certifications_type CHECK (certification_type IN (
        'RECREATIONAL', 'SPECIALTY', 'TECHNICAL', 'PROFESSIONAL', 'FIRST_AID', 'INSURANCE', 'OTHER'
    )),
    CONSTRAINT ck_certifications_dates CHECK (expiry_date IS NULL OR expiry_date > issue_date)
);

CREATE INDEX idx_certifications_owner_issue_date ON certifications (owner_id, issue_date DESC);
CREATE INDEX idx_certifications_expiry_date ON certifications (expiry_date) WHERE expiry_date IS NOT NULL;