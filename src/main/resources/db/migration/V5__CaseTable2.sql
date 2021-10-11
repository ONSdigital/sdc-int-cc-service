
DROP TABLE caze;

CREATE TABLE caze (
    id UUID,
    survey_id UUID NOT NULL,
    collection_exercise_id UUID NOT NULL,
    case_ref INT8,
    invalid BOOLEAN DEFAULT false,
    refusal_received TEXT,

    -- address
    uprn VARCHAR(16) NOT NULL,
    address_line1 TEXT NOT NULL,
    address_line2 TEXT,
    address_line3 TEXT,
    town_name TEXT NOT NULL,
    region TEXT NOT NULL,
    postcode TEXT NOT NULL,

    -- contact
    phone_number TEXT,

    PRIMARY KEY (id)
);

CREATE INDEX caze_case_ref_idx ON caze (case_ref);
CREATE INDEX caze_uprn_idx ON caze (uprn);

