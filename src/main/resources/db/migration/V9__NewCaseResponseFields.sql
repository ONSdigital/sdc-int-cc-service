ALTER TABLE case_response DROP COLUMN cohort;
ALTER TABLE case_response DROP COLUMN responded;

ALTER TABLE case_response ADD COLUMN receipt_received boolean NOT NULL DEFAULT false;
ALTER TABLE case_response ADD COLUMN eq_launched boolean NOT NULL  DEFAULT false;
ALTER TABLE case_response ADD COLUMN uac_hash text NOT NULL  DEFAULT '';
ALTER TABLE case_response ADD COLUMN questionnaire VARCHAR NOT NULL  DEFAULT '';
ALTER TABLE case_response ADD COLUMN active VARCHAR NOT NULL  DEFAULT false;

ALTER TABLE case_response ALTER COLUMN receipt_received DROP DEFAULT;
ALTER TABLE case_response ALTER COLUMN eq_launched DROP DEFAULT;
ALTER TABLE case_response ALTER COLUMN uac_hash DROP DEFAULT;
ALTER TABLE case_response ALTER COLUMN questionnaire DROP DEFAULT;
ALTER TABLE case_response ALTER COLUMN active DROP DEFAULT;


