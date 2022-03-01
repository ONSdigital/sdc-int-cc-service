ALTER TABLE cc_schema.ccuser RENAME COLUMN name TO identity;

ALTER TABLE cc_schema.ccuser ADD COLUMN forename TEXT DEFAULT '';
ALTER TABLE cc_schema.ccuser ADD COLUMN surname TEXT DEFAULT '';
