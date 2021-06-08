-- Creation

CREATE TABLE caze (
    case_id uuid NOT null,
    case_ref INT8,
    case_type VARCHAR(255),
    uprn VARCHAR(255),
    PRIMARY KEY (case_id)
);

CREATE INDEX caze_case_ref_idx ON caze (case_ref);

-- EOF
