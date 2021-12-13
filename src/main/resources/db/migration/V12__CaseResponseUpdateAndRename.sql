DROP TABLE case_response;

CREATE TABLE uac (
   id UUID PRIMARY KEY,
   case_id UUID REFERENCES collection_case NOT NULL,
   wave_num INTEGER NOT NULL,
   active BOOLEAN NOT NULL,
   receipt_received BOOLEAN NOT NULL,
   eq_launched BOOLEAN NOT NULL,
   uac_hash TEXT NOT NULL,
   questionnaire TEXT NOT NULL,
   collection_exercise_id uuid NOT NULL,
   survey_id uuid NOT NULL,
   collection_instrument_url TEXT NOT NULL
);

ALTER TABLE collection_case ADD COLUMN cc_status TEXT NOT NULL DEFAULT '';
ALTER TABLE collection_case ALTER COLUMN cc_status DROP DEFAULT;
