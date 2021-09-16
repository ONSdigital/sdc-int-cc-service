DROP TABLE message_to_send;

CREATE TABLE event_to_send (
      id UUID NOT NULL,
      event_type VARCHAR(255) NOT NULL,
      payload TEXT NOT NULL,
      PRIMARY KEY (id)
);
