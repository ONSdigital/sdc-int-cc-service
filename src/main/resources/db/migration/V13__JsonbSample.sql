ALTER TABLE cc_schema.collection_case ADD COLUMN sample JSONB NOT NULL DEFAULT '{}';
ALTER TABLE cc_schema.collection_case ADD COLUMN sample_sensitive JSONB NOT NULL DEFAULT '{}';

ALTER TABLE cc_schema.collection_case ALTER COLUMN sample DROP DEFAULT;
ALTER TABLE cc_schema.collection_case ALTER COLUMN sample_sensitive DROP DEFAULT;

ALTER TABLE cc_schema.collection_case DROP COLUMN uprn;
ALTER TABLE cc_schema.collection_case DROP COLUMN address_line1;
ALTER TABLE cc_schema.collection_case DROP COLUMN address_line2;
ALTER TABLE cc_schema.collection_case DROP COLUMN address_line3;
ALTER TABLE cc_schema.collection_case DROP COLUMN town_name;
ALTER TABLE cc_schema.collection_case DROP COLUMN postcode;
ALTER TABLE cc_schema.collection_case DROP COLUMN region;
ALTER TABLE cc_schema.collection_case DROP COLUMN questionnaire;
ALTER TABLE cc_schema.collection_case DROP COLUMN sample_unit_ref;
ALTER TABLE cc_schema.collection_case DROP COLUMN cohort;
ALTER TABLE cc_schema.collection_case DROP COLUMN gor9d;
ALTER TABLE cc_schema.collection_case DROP COLUMN la_code;
ALTER TABLE cc_schema.collection_case DROP COLUMN uprn_latitude;
ALTER TABLE cc_schema.collection_case DROP COLUMN uprn_longitude;

ALTER TABLE cc_schema.collection_case DROP COLUMN phone_number;
