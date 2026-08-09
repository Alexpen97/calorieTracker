-- User product feedback submissions (ULT-13)
CREATE TABLE user_feedback (
    id           UUID PRIMARY KEY,
    user_id      UUID NOT NULL REFERENCES app_user(id),
    message      TEXT NOT NULL,
    status       VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    app_version  VARCHAR(64) NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_user_feedback_user_created ON user_feedback(user_id, created_at DESC);
