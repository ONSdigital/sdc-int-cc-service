-- add a table to hold products for a survey.

CREATE TABLE product (
  id UUID PRIMARY KEY,
  survey_id UUID REFERENCES survey NOT NULL,
  delivery_channel TEXT NOT NULL,
  product_group TEXT,
  pack_code TEXT NOT NULL,
  description TEXT NOT NULL,
  metadata JSONB
);

ALTER TABLE survey
  ADD COLUMN metadata JSONB;
