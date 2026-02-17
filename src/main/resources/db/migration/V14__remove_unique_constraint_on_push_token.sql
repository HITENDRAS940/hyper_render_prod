ALTER TABLE admin_push_tokens DROP CONSTRAINT IF EXISTS admin_push_tokens_token_key;
ALTER TABLE admin_push_tokens DROP CONSTRAINT IF EXISTS uk_admin_push_tokens_token; -- In case hibernate generated it with this name
-- Drop index if it was created on token specifically for the unique constraint, though postgres usually manages it with the constraint.
-- Just dropping constraint removes the unique index.

