ALTER TABLE collection_case
  ADD COLUMN questionnaire TEXT,
  ADD COLUMN sample_unit_ref TEXT,
  ADD COLUMN cohort INTEGER,

  -- address
  ADD COLUMN gor9d TEXT,            -- LFS geographic region
  ADD COLUMN la_code TEXT,          -- local area code
  ADD COLUMN uprn_latitude TEXT,
  ADD COLUMN uprn_longitude TEXT;

