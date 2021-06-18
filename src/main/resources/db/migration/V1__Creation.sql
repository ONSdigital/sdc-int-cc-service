-- Creation of sample table

CREATE TABLE caze (
    case_id uuid NOT null,
    case_ref INT8,
    case_type VARCHAR(255),
    uprn VARCHAR(255),
    PRIMARY KEY (case_id)
);

CREATE INDEX caze_case_ref_idx ON caze (case_ref);

-- add case data

INSERT INTO caze (case_id, case_ref, case_type, uprn)
	VALUES ('fae3f57c-c54c-11eb-9d99-4c3275913db5', 100, 'HH', '1234');
INSERT INTO caze (case_id, case_ref, case_type, uprn)
	VALUES ('430a9342-c54d-11eb-abce-4c3275913db5', 200, 'CE', '2345');

-- EOF
