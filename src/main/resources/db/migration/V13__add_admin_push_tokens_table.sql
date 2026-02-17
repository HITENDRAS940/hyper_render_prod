CREATE TABLE admin_push_tokens (
    id BIGSERIAL PRIMARY KEY,
    admin_id BIGINT NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_admin_push_tokens_admin_id ON admin_push_tokens(admin_id);

