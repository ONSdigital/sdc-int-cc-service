-- Creation

CREATE TABLE caze (
    case_id uuid NOT null,
    case_ref INT8,
    case_type VARCHAR(255),
    uprn VARCHAR(255),
    PRIMARY KEY (case_id)
);

CREATE INDEX caze_case_ref_idx ON caze (case_ref);

GRANT USAGE ON SCHEMA cc_schema TO ccsvc;
GRANT SELECT, UPDATE, INSERT, DELETE ON ALL TABLES IN SCHEMA cc_schema TO ccsvc;
GRANT SELECT, UPDATE ON ALL SEQUENCES IN SCHEMA cc_schema TO ccsvc;


-- EOF
