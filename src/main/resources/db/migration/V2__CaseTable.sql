
DROP TABLE caze;

CREATE TYPE CASE_TYPE_ENUM AS ENUM('HH', 'HI', 'CE', 'SPG');
CREATE CAST (VARCHAR AS CASE_TYPE_ENUM) WITH INOUT AS IMPLICIT;

CREATE TABLE caze (
    id UUID NOT NULL,
    case_ref INT8,
    case_type CASE_TYPE_ENUM,
    survey VARCHAR(255) NOT NULL,
    collection_exercise_id UUID,
    actionable_from VARCHAR(255),
    hand_delivery BOOLEAN DEFAULT false,
    address_invalid BOOLEAN DEFAULT false NOT NULL,
    ce_expected_capacity INT4,

    -- contact
    title VARCHAR(255),
    forename VARCHAR(255),
    surname VARCHAR(255),
    tel_no VARCHAR(255),

    -- address
    uprn VARCHAR(255),
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    address_line3 VARCHAR(255),
    town_name VARCHAR(255),
    postcode VARCHAR(255),
    region VARCHAR(255),
    estab_type VARCHAR(255),
    organisation_name VARCHAR(255),
    latitude VARCHAR(255),
    longitude VARCHAR(255),
    estab_uprn VARCHAR(255),
    address_type VARCHAR(255),
    address_level VARCHAR(255),
    --
    created_date_time TIMESTAMP WITH TIME ZONE,

    PRIMARY KEY (id)
);

CREATE INDEX caze_case_ref_idx ON caze (case_ref);
CREATE INDEX caze_uprn_idx ON caze (uprn);

