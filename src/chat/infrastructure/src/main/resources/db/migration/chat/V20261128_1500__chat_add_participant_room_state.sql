ALTER TABLE chat_participant
    ADD COLUMN notification_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN last_read_message_id BIGINT;
