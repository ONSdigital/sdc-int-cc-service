ALTER TABLE event_to_send RENAME COLUMN insertion_time TO created_date_time;
ALTER TABLE case_interaction RENAME COLUMN insertion_time TO date_time;
ALTER TABLE case_appointment RENAME COLUMN insertion_time TO date_time;

CREATE INDEX event_to_send_created_date_time_idx ON event_to_send (created_date_time);