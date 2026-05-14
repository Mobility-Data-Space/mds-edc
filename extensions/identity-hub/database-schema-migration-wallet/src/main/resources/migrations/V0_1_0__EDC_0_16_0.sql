ALTER TABLE participant_context ADD COLUMN identity TEXT;
ALTER TABLE participant_context DROP COLUMN api_token_alias;