
DROP TABLE IF EXISTS survey CASCADE;
DROP TABLE IF EXISTS collection_exercise CASCADE;
DROP TABLE IF EXISTS caze CASCADE;
DROP TABLE IF EXISTS collection_case CASCADE;
DROP TABLE IF EXISTS case_response CASCADE;
DROP TABLE IF EXISTS operator CASCADE;
DROP TABLE IF EXISTS case_interaction CASCADE;
DROP TABLE IF EXISTS case_appointment CASCADE;
DROP TABLE IF EXISTS role CASCADE;
DROP TABLE IF EXISTS operator_role CASCADE;
DROP TABLE IF EXISTS admin_role CASCADE;
DROP TABLE IF EXISTS permission CASCADE;

CREATE TABLE survey (
	id UUID PRIMARY KEY,
	name TEXT NOT NULL,
	sample_definition JSONB NOT NULL,
	sample_definition_url TEXT NOT NULL
);

CREATE TABLE collection_exercise (
	id UUID PRIMARY KEY,
	survey_id UUID REFERENCES survey NOT NULL,
	name TEXT NOT NULL,
	reference TEXT NOT NULL,
	start_date TIMESTAMP NOT NULL,
	end_date TIMESTAMP NOT NULL,
	cohort_schedule INTEGER NOT NULL,
	cohorts INTEGER NOT NULL,
	number_of_waves INTEGER NOT NULL,
	wave_length INTEGER NOT NULL
);

-- Named 'collection_case' since 'case' is a reserved word.
CREATE TABLE collection_case (
	id UUID PRIMARY KEY,
	collection_exercise_id UUID REFERENCES collection_exercise NOT NULL,
	case_ref TEXT UNIQUE NOT NULL,
	invalid BOOLEAN DEFAULT false NOT NULL,
	refusal_received TEXT,
	created_at TIMESTAMP NOT NULL,
	last_updated_at TIMESTAMP NOT NULL,

	-- address
	uprn TEXT NOT NULL,
	address_line1 TEXT NOT NULL,
	address_line2 TEXT,
	address_line3 TEXT,
	town_name TEXT NOT NULL,
	postcode TEXT NOT NULL,
	region TEXT NOT NULL,

	-- contact
	phone_number TEXT
);

CREATE INDEX collection_case_case_ref_idx ON collection_case (case_ref);
CREATE INDEX collection_case_uprn_idx ON collection_case (uprn);

CREATE TABLE case_response (
	id UUID PRIMARY KEY,
	case_id UUID REFERENCES collection_case NOT NULL,
	responded BOOLEAN DEFAULT FALSE NOT NULL,
	wave_num INTEGER NOT NULL,
	cohort INTEGER NOT NULL
);

-- Named 'operator' since 'user' is a reserved word.
CREATE TABLE operator (
	id UUID PRIMARY KEY,
	name TEXT NOT NULL,
	active BOOLEAN DEFAULT TRUE NOT NULL
);

CREATE TABLE case_interaction (
	id UUID PRIMARY KEY,
	case_id UUID REFERENCES collection_case NOT NULL,
	operator_id UUID REFERENCES operator NOT NULL,
	date_time TIMESTAMP NOT NULL,
	type TEXT NOT NULL,
	outcome TEXT NOT NULL,
	note TEXT
);

CREATE TABLE case_appointment (
	id UUID PRIMARY KEY,
	case_id UUID REFERENCES collection_case NOT NULL,
	interaction_id UUID REFERENCES case_interaction NOT NULL,
	type TEXT NOT NULL,
	date_time TIMESTAMP,
	start_date DATE,
	end_date DATE,
	start_time TIME,
	end_time TIME,
	days_of_week TEXT NOT NULL
);

CREATE TABLE role (
	id UUID PRIMARY KEY,
	name TEXT NOT NULL
);

CREATE TABLE operator_role (
	id UUID PRIMARY KEY,
	role_id UUID REFERENCES role NOT NULL,
	operator_id UUID REFERENCES operator NOT NULL
);

CREATE TABLE admin_role (
	id UUID PRIMARY KEY,
	role_id UUID REFERENCES role NOT NULL,
	operator_id UUID REFERENCES operator NOT NULL
);

CREATE TABLE permission (
	id UUID PRIMARY KEY,
	role_id UUID REFERENCES role NOT NULL,
	authorised_activity TEXT NOT NULL
);

-- dummy data population to assist testing --

INSERT INTO survey (id, name, sample_definition, sample_definition_url)
VALUES (
    'fae3f57c-c54c-11eb-9d99-4c3275913db5',
    'dummy-survey',
    '{"dummy": "blah"}'::jsonb,
    'https://raw.githubusercontent.com/ONSdigital/ssdc-shared-events/main/sample/social/0.1.0-DRAFT/social.json'
);

INSERT INTO collection_exercise (id, survey_id, name, reference,
    start_date, end_date, cohort_schedule, cohorts, number_of_waves, wave_length)
VALUES (
    '430a9342-c54d-11eb-abce-4c3275913db5',
    'fae3f57c-c54c-11eb-9d99-4c3275913db5',
    'dummy-coll-ex',
    'dummy-ref',
    '2021-10-12 19:10:25',
    '2022-11-12 19:10:25',
    1,
    2,
    3,
    4
);

