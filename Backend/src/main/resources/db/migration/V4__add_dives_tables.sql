CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE users
    ADD COLUMN dive_number_offset INTEGER NOT NULL DEFAULT 0,
    ADD CONSTRAINT ck_users_dive_number_offset CHECK (dive_number_offset >= 0);

CREATE TABLE dive_sites (
    id            UUID           PRIMARY KEY,
    owner_id      UUID,
    name          VARCHAR(150)   NOT NULL,
    locality      VARCHAR(150),
    country_code  VARCHAR(2),
    latitude      DOUBLE PRECISION NOT NULL,
    longitude     DOUBLE PRECISION NOT NULL,
    site_type     VARCHAR(20),
    min_depth_m   DOUBLE PRECISION,
    max_depth_m   DOUBLE PRECISION,
    description   VARCHAR(2000),
    source        VARCHAR(20)    NOT NULL DEFAULT 'USER',
    external_id   VARCHAR(100),
    created_at    TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ    NOT NULL DEFAULT now(),

    CONSTRAINT fk_dive_sites_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_dive_sites_source_external UNIQUE (source, external_id),
    CONSTRAINT ck_dive_sites_latitude  CHECK (latitude  BETWEEN -90  AND 90),
    CONSTRAINT ck_dive_sites_longitude CHECK (longitude BETWEEN -180 AND 180),
    CONSTRAINT ck_dive_sites_depths CHECK (
        (min_depth_m IS NULL OR min_depth_m >= 0) AND
        (max_depth_m IS NULL OR max_depth_m > 0) AND
        (min_depth_m IS NULL OR max_depth_m IS NULL OR min_depth_m <= max_depth_m)
    ),
    CONSTRAINT ck_dive_sites_site_type CHECK (site_type IS NULL OR site_type IN (
         'REEF', 'WALL', 'WRECK', 'SHOAL', 'CAVE', 'LAKE', 'QUARRY', 'OTHER'
    )),
    CONSTRAINT ck_dive_sites_source CHECK (source IN ('USER', 'OPENSTREETMAP')),
    CONSTRAINT ck_dive_sites_ownership CHECK (
        (source = 'USER' AND owner_id IS NOT NULL AND external_id IS NULL) OR
        (source <> 'USER' AND owner_id IS NULL AND external_id IS NOT NULL)
    )
);

CREATE INDEX idx_dive_sites_owner_id ON dive_sites (owner_id);

CREATE TABLE dives (
    id                           UUID             PRIMARY KEY,
    owner_id                     UUID             NOT NULL,
    site_id                      UUID             NOT NULL,

    started_at                   TIMESTAMPTZ      NOT NULL,
    ended_at                     TIMESTAMPTZ      NOT NULL,
    time_zone                    VARCHAR(64)      NOT NULL,

    purpose                      VARCHAR(20),
    entry_method                 VARCHAR(20),

    weather                      VARCHAR(20),
    sea_state                    VARCHAR(20),
    current_strength             VARCHAR(20),
    visibility_m                 DOUBLE PRECISION,
    air_temperature_c            DOUBLE PRECISION,
    surface_water_temperature_c  DOUBLE PRECISION,
    bottom_water_temperature_c   DOUBLE PRECISION,
    water_type                   VARCHAR(10)      NOT NULL DEFAULT 'SALT',

    max_depth_m                  DOUBLE PRECISION NOT NULL,
    avg_depth_m                  DOUBLE PRECISION,
    weight_kg                    DOUBLE PRECISION,

    residual_nitrogen_start      VARCHAR(10),
    residual_nitrogen_end        VARCHAR(10),
    residual_nitrogen_rate       DOUBLE PRECISION,

    segments                     JSONB            NOT NULL DEFAULT '[]'::jsonb,

    gas_used_l                   DOUBLE PRECISION,
    rmv_l_min                    DOUBLE PRECISION,

    symptoms_notes               VARCHAR(1000),
    rating                       INTEGER,
    notes                        VARCHAR(4000),

    created_at                   TIMESTAMPTZ      NOT NULL DEFAULT now(),
    updated_at                   TIMESTAMPTZ      NOT NULL DEFAULT now(),

    CONSTRAINT fk_dives_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_dives_site FOREIGN KEY (site_id) REFERENCES dive_sites (id) ON DELETE RESTRICT,
    CONSTRAINT ck_dives_times CHECK (ended_at > started_at),
    CONSTRAINT ex_dives_no_overlap EXCLUDE USING gist (
        owner_id WITH =,
        tstzrange(started_at, ended_at, '[)') WITH &&
    ),

    CONSTRAINT ck_dives_purpose CHECK (purpose IS NULL OR purpose IN (
         'RECREATIONAL', 'TRAINING', 'EXPLORATION', 'PHOTOGRAPHY', 'SCIENTIFIC', 'WORK'
    )),
    CONSTRAINT ck_dives_entry_method CHECK (entry_method IS NULL OR entry_method IN ('SHORE', 'BOAT')),
    CONSTRAINT ck_dives_weather CHECK (weather IS NULL OR weather IN (
        'SUNNY', 'PARTLY_CLOUDY', 'CLOUDY', 'RAIN', 'STORM'
    )),
    CONSTRAINT ck_dives_sea_state CHECK (sea_state IS NULL OR sea_state IN ('CALM', 'SLIGHT', 'MODERATE', 'ROUGH')),
    CONSTRAINT ck_dives_current CHECK (current_strength IS NULL OR current_strength IN ('NONE', 'LIGHT', 'MODERATE', 'STRONG')),
    CONSTRAINT ck_dives_water_type CHECK (water_type IN ('SALT', 'FRESH')),

    CONSTRAINT ck_dives_max_depth CHECK (max_depth_m > 0 AND max_depth_m <= 350),
    CONSTRAINT ck_dives_avg_depth CHECK (avg_depth_m IS NULL OR (avg_depth_m > 0 AND avg_depth_m <= max_depth_m)),
    CONSTRAINT ck_dives_visibility CHECK (visibility_m IS NULL OR visibility_m >= 0),
    CONSTRAINT ck_dives_weight CHECK (weight_kg IS NULL OR weight_kg >= 0),
    CONSTRAINT ck_dives_temperatures CHECK (
        (air_temperature_c IS NULL OR air_temperature_c BETWEEN -50 AND 60) AND
        (surface_water_temperature_c IS NULL OR surface_water_temperature_c BETWEEN -3 AND 40) AND
        (bottom_water_temperature_c IS NULL OR bottom_water_temperature_c BETWEEN -3 AND 40)
    ),
    CONSTRAINT ck_dives_rating CHECK (rating IS NULL OR rating BETWEEN 1 AND 5),
    CONSTRAINT ck_dives_consumption CHECK (
        (gas_used_l IS NULL OR gas_used_l >= 0) AND
        (rmv_l_min IS NULL OR rmv_l_min >= 0)
    ),
    CONSTRAINT ck_dives_segments_array CHECK (jsonb_typeof(segments) = 'array')
);

CREATE INDEX idx_dives_owner_started_at ON dives (owner_id, started_at);
CREATE INDEX idx_dives_site_id ON dives (site_id);

CREATE TABLE dive_tags (
    dive_id  UUID         NOT NULL,
    tag      VARCHAR(20)  NOT NULL,

    CONSTRAINT pk_dive_tags PRIMARY KEY (dive_id, tag),
    CONSTRAINT fk_dive_tags_dive FOREIGN KEY (dive_id) REFERENCES dives (id) ON DELETE CASCADE,
    CONSTRAINT ck_dive_tags_tag CHECK (tag IN (
        'NIGHT', 'DRIFT', 'DEEP', 'WRECK', 'WALL', 'SHOAL', 'CAVE', 'ICE', 'ALTITUDE', 'DECOMPRESSION'
    ))
);

CREATE TABLE dive_symptoms_before (
    dive_id  UUID         NOT NULL,
    symptom  VARCHAR(30)  NOT NULL,

    CONSTRAINT pk_dive_symptoms_before PRIMARY KEY (dive_id, symptom),
    CONSTRAINT fk_dive_symptoms_before_dive FOREIGN KEY (dive_id) REFERENCES dives (id) ON DELETE CASCADE,
    CONSTRAINT ck_dive_symptoms_before_symptom CHECK (symptom IN (
        'FATIGUE', 'HEADACHE', 'NAUSEA', 'EAR_PAIN', 'SINUS_PAIN', 'JOINT_PAIN',
        'SKIN_RASH_ITCHING', 'TINGLING_NUMBNESS', 'DIZZINESS', 'SHORTNESS_OF_BREATH', 'COLD'
    ))
);

CREATE TABLE dive_symptoms_after (
    dive_id  UUID         NOT NULL,
    symptom  VARCHAR(30)  NOT NULL,

    CONSTRAINT pk_dive_symptoms_after PRIMARY KEY (dive_id, symptom),
    CONSTRAINT fk_dive_symptoms_after_dive FOREIGN KEY (dive_id) REFERENCES dives (id) ON DELETE CASCADE,
    CONSTRAINT ck_dive_symptoms_after_symptom CHECK (symptom IN (
        'FATIGUE', 'HEADACHE', 'NAUSEA', 'EAR_PAIN', 'SINUS_PAIN', 'JOINT_PAIN',
        'SKIN_RASH_ITCHING', 'TINGLING_NUMBNESS', 'DIZZINESS', 'SHORTNESS_OF_BREATH', 'COLD'
    ))
);

CREATE TABLE dive_tanks (
    id                    UUID             PRIMARY KEY,
    dive_id               UUID             NOT NULL,
    position              INTEGER          NOT NULL,
    volume_l              DOUBLE PRECISION NOT NULL,
    working_pressure_bar  INTEGER,
    material              VARCHAR(20),
    start_pressure_bar    INTEGER          NOT NULL,
    end_pressure_bar      INTEGER          NOT NULL,
    o2_percent            DOUBLE PRECISION NOT NULL DEFAULT 21,
    he_percent            DOUBLE PRECISION NOT NULL DEFAULT 0,

    CONSTRAINT fk_dive_tanks_dive FOREIGN KEY (dive_id)
    REFERENCES dives (id) ON DELETE CASCADE,
    CONSTRAINT ck_dive_tanks_position CHECK (position >= 0),
    CONSTRAINT ck_dive_tanks_volume CHECK (volume_l > 0 AND volume_l <= 30),
    CONSTRAINT ck_dive_tanks_working_pressure CHECK (working_pressure_bar IS NULL OR working_pressure_bar BETWEEN 100 AND 350),
    CONSTRAINT ck_dive_tanks_material CHECK (material IS NULL OR material IN ('STEEL', 'ALUMINIUM')),
    CONSTRAINT ck_dive_tanks_pressures CHECK (
        start_pressure_bar > 0 AND start_pressure_bar <= 350 AND
        end_pressure_bar >= 0 AND end_pressure_bar <= start_pressure_bar
    ),
    CONSTRAINT ck_dive_tanks_gas CHECK (
        o2_percent BETWEEN 5 AND 100 AND
        he_percent BETWEEN 0 AND 95 AND
        o2_percent + he_percent <= 100
    )
);

CREATE INDEX idx_dive_tanks_dive_id ON dive_tanks (dive_id);

CREATE TABLE dive_gear (
    id          UUID          PRIMARY KEY,
    dive_id     UUID          NOT NULL,
    position    INTEGER       NOT NULL,
    gear_id     UUID,
    category    VARCHAR(30)   NOT NULL,
    name        VARCHAR(100)  NOT NULL,
    brand       VARCHAR(100),
    model       VARCHAR(100),
    attributes  JSONB         NOT NULL DEFAULT '{}'::jsonb,

    CONSTRAINT fk_dive_gear_dive FOREIGN KEY (dive_id) REFERENCES dives (id) ON DELETE CASCADE,
    CONSTRAINT fk_dive_gear_gear FOREIGN KEY (gear_id) REFERENCES gears (id) ON DELETE SET NULL,
    CONSTRAINT ck_dive_gear_position CHECK (position >= 0),
    CONSTRAINT ck_dive_gear_attributes_object CHECK (jsonb_typeof(attributes) = 'object')
);

CREATE INDEX idx_dive_gear_dive_id ON dive_gear (dive_id);
CREATE INDEX idx_dive_gear_gear_id ON dive_gear (gear_id);