ALTER TABLE cc_schema.case_interaction RENAME COLUMN outcome TO subtype;
ALTER TABLE cc_schema.case_interaction ALTER COLUMN subtype DROP NOT NULL;