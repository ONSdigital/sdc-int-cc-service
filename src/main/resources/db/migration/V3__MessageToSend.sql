CREATE TABLE message_to_send (
      id UUID NOT NULL,
      destination_topic VARCHAR(255) NOT NULL,
      message_body BYTEA NOT NULL,
      PRIMARY KEY (id)
);