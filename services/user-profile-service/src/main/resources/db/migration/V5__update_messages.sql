-- Server-driven in-app update messages (ULT-12)

CREATE TABLE update_message (
    id            UUID PRIMARY KEY,
    title         TEXT NOT NULL,
    body          TEXT NOT NULL,
    image_url     TEXT NULL,
    action_label  TEXT NULL,
    action_url    TEXT NULL,
    pushed_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_update_message_pushed_at ON update_message(pushed_at ASC);

CREATE TABLE update_message_ack (
    user_id           UUID NOT NULL REFERENCES app_user(id),
    message_id        UUID NOT NULL REFERENCES update_message(id),
    acknowledged_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (user_id, message_id)
);

CREATE INDEX idx_update_message_ack_user ON update_message_ack(user_id);
