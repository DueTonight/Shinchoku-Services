-- Retain historical foreign keys while withdrawing tenant access.
ALTER TABLE workspace_members ADD COLUMN removed_at timestamptz;
ALTER TABLE sessions ADD COLUMN mfa_verified boolean NOT NULL DEFAULT false;
CREATE INDEX sessions_user_active ON sessions (user_id, expires_at) WHERE revoked_at IS NULL;
CREATE INDEX api_keys_owner_active ON api_keys (workspace_id, owner_id) WHERE revoked_at IS NULL;
CREATE INDEX comments_issue_page ON comments (workspace_id, issue_id, created_at, id) WHERE deleted_at IS NULL;
CREATE INDEX triage_target_page ON triage_requests (workspace_id, target_team_id, created_at, id);

ALTER TABLE team_members ADD COLUMN removed_at timestamptz;
