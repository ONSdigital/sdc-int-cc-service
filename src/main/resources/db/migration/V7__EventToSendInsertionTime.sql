ALTER TABLE event_to_send ADD column insertion_time TIMESTAMP NOT NULL DEFAULT '2021-01-01 00:00:00.000';
ALTER TABLE event_to_send ALTER COLUMN insertion_time DROP DEFAULT;