-- Proposed PostgreSQL 15+ schema. Design artifact only; not applied to the app.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE users (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  email text NOT NULL CHECK (email = lower(email)),
  display_name text NOT NULL CHECK (length(trim(display_name)) > 0),
  avatar_url text,
  timezone text NOT NULL DEFAULT 'UTC',
  theme text NOT NULL DEFAULT 'system' CHECK (theme IN ('light','dark','system')),
  created_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (email)
);

CREATE TABLE workspaces (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  name text NOT NULL CHECK (length(trim(name)) > 0),
  slug text NOT NULL UNIQUE,
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE workspace_members (
  workspace_id uuid NOT NULL REFERENCES workspaces(id),
  user_id uuid NOT NULL REFERENCES users(id),
  role text NOT NULL CHECK (role IN ('owner','admin','member','viewer','guest')),
  joined_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (workspace_id, user_id)
);

CREATE TABLE teams (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  workspace_id uuid NOT NULL REFERENCES workspaces(id),
  name text NOT NULL CHECK (length(trim(name)) > 0),
  key text NOT NULL CHECK (key ~ '^[A-Z][A-Z0-9]{1,9}$'),
  description text NOT NULL DEFAULT '',
  next_issue_number bigint NOT NULL DEFAULT 1 CHECK (next_issue_number > 0),
  archived_at timestamptz,
  UNIQUE (workspace_id, id),
  UNIQUE (workspace_id, key)
);

CREATE TABLE team_members (
  workspace_id uuid NOT NULL,
  team_id uuid NOT NULL,
  user_id uuid NOT NULL,
  role text NOT NULL DEFAULT 'member' CHECK (role IN ('lead','member','viewer')),
  PRIMARY KEY (workspace_id, team_id, user_id),
  FOREIGN KEY (workspace_id, team_id) REFERENCES teams(workspace_id, id),
  FOREIGN KEY (workspace_id, user_id) REFERENCES workspace_members(workspace_id, user_id)
);

-- Workspace project portfolio and team epics share one entity.
-- A project has one owning team; a portfolio lists projects from all teams.
CREATE TABLE projects (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  workspace_id uuid NOT NULL,
  team_id uuid NOT NULL,
  name text NOT NULL CHECK (length(trim(name)) > 0),
  description text NOT NULL DEFAULT '',
  state text NOT NULL DEFAULT 'planned' CHECK (state IN ('planned','active','paused','completed','cancelled')),
  lead_id uuid,
  planned_start date,
  target_date date,
  planning_notes text NOT NULL DEFAULT '',
  archived_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (workspace_id, team_id, id),
  FOREIGN KEY (workspace_id, team_id) REFERENCES teams(workspace_id, id),
  FOREIGN KEY (workspace_id, lead_id) REFERENCES workspace_members(workspace_id, user_id),
  CHECK (target_date IS NULL OR planned_start IS NULL OR target_date >= planned_start)
);

CREATE TABLE milestones (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  workspace_id uuid NOT NULL,
  team_id uuid NOT NULL,
  project_id uuid NOT NULL,
  name text NOT NULL CHECK (length(trim(name)) > 0),
  target_date date NOT NULL,
  FOREIGN KEY (workspace_id, team_id, project_id) REFERENCES projects(workspace_id, team_id, id)
);

CREATE TABLE cycles (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  workspace_id uuid NOT NULL,
  team_id uuid NOT NULL,
  number integer NOT NULL CHECK (number > 0),
  starts_on date NOT NULL,
  ends_on date NOT NULL,
  UNIQUE (workspace_id, team_id, id),
  UNIQUE (workspace_id, team_id, number),
  FOREIGN KEY (workspace_id, team_id) REFERENCES teams(workspace_id, id),
  -- Inclusive two-week sprint. API serializes starts/ends as dates, not instants.
  CHECK (ends_on = starts_on + 13)
);

CREATE TABLE labels (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  workspace_id uuid NOT NULL REFERENCES workspaces(id),
  name text NOT NULL CHECK (length(trim(name)) > 0),
  UNIQUE (workspace_id, id)
);
CREATE UNIQUE INDEX labels_workspace_name ON labels (workspace_id, lower(name));

CREATE TABLE issues (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  workspace_id uuid NOT NULL,
  team_id uuid NOT NULL,
  number bigint NOT NULL CHECK (number > 0),
  title text NOT NULL CHECK (length(trim(title)) > 0),
  description text NOT NULL DEFAULT '',
  state text NOT NULL DEFAULT 'todo' CHECK (state IN ('upcoming','todo','in_progress','in_review','blocked','done','cancelled')),
  priority text NOT NULL DEFAULT 'medium' CHECK (priority IN ('urgent','high','medium','low','none')),
  assignee_id uuid,
  creator_id uuid NOT NULL,
  project_id uuid,
  cycle_id uuid,
  due_on date,
  completed_at timestamptz,
  archived_at timestamptz,
  version integer NOT NULL DEFAULT 1 CHECK (version > 0),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (workspace_id, id),
  UNIQUE (workspace_id, team_id, number),
  FOREIGN KEY (workspace_id, team_id) REFERENCES teams(workspace_id, id),
  FOREIGN KEY (workspace_id, assignee_id) REFERENCES workspace_members(workspace_id, user_id),
  FOREIGN KEY (workspace_id, creator_id) REFERENCES workspace_members(workspace_id, user_id),
  FOREIGN KEY (workspace_id, team_id, project_id) REFERENCES projects(workspace_id, team_id, id),
  FOREIGN KEY (workspace_id, team_id, cycle_id) REFERENCES cycles(workspace_id, team_id, id),
  CHECK ((state = 'done') = (completed_at IS NOT NULL))
);

CREATE TABLE issue_labels (
  workspace_id uuid NOT NULL,
  issue_id uuid NOT NULL,
  label_id uuid NOT NULL,
  PRIMARY KEY (workspace_id, issue_id, label_id),
  FOREIGN KEY (workspace_id, issue_id) REFERENCES issues(workspace_id, id) ON DELETE CASCADE,
  FOREIGN KEY (workspace_id, label_id) REFERENCES labels(workspace_id, id)
);
CREATE INDEX issue_labels_filter ON issue_labels (workspace_id, label_id, issue_id);
CREATE INDEX issues_team_state ON issues (workspace_id, team_id, state, updated_at DESC, id);
CREATE INDEX issues_assignee ON issues (workspace_id, assignee_id, updated_at DESC, id);
CREATE INDEX issues_cycle ON issues (workspace_id, cycle_id);
CREATE INDEX issues_project ON issues (workspace_id, project_id);

CREATE TABLE comments (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  workspace_id uuid NOT NULL,
  issue_id uuid NOT NULL,
  author_id uuid NOT NULL,
  body text NOT NULL CHECK (length(trim(body)) > 0),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  deleted_at timestamptz,
  FOREIGN KEY (workspace_id, issue_id) REFERENCES issues(workspace_id, id),
  FOREIGN KEY (workspace_id, author_id) REFERENCES workspace_members(workspace_id, user_id)
);

CREATE TABLE issue_activity (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  workspace_id uuid NOT NULL,
  issue_id uuid NOT NULL,
  actor_id uuid,
  kind text NOT NULL CHECK (kind IN ('created','updated','label_added','label_removed','commented','synced')),
  changes jsonb NOT NULL DEFAULT '{}',
  occurred_at timestamptz NOT NULL DEFAULT now(),
  FOREIGN KEY (workspace_id, issue_id) REFERENCES issues(workspace_id, id),
  FOREIGN KEY (workspace_id, actor_id) REFERENCES workspace_members(workspace_id, user_id)
);
CREATE INDEX activity_issue_time ON issue_activity (workspace_id, issue_id, occurred_at, id);

CREATE TABLE saved_views (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  workspace_id uuid NOT NULL,
  owner_id uuid NOT NULL,
  name text NOT NULL CHECK (length(trim(name)) > 0),
  visibility text NOT NULL DEFAULT 'personal' CHECK (visibility IN ('personal','workspace')),
  label_match text NOT NULL DEFAULT 'any' CHECK (label_match IN ('any','all')),
  search text NOT NULL DEFAULT '',
  states text[] NOT NULL DEFAULT '{}'
    CHECK (states <@ ARRAY['upcoming','todo','in_progress','in_review','blocked','done','cancelled']::text[]),
  assignee_id uuid,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (workspace_id, id),
  FOREIGN KEY (workspace_id, owner_id) REFERENCES workspace_members(workspace_id, user_id),
  FOREIGN KEY (workspace_id, assignee_id) REFERENCES workspace_members(workspace_id, user_id)
);
CREATE TABLE saved_view_labels (
  workspace_id uuid NOT NULL,
  view_id uuid NOT NULL,
  label_id uuid NOT NULL,
  PRIMARY KEY (workspace_id, view_id, label_id),
  FOREIGN KEY (workspace_id, view_id) REFERENCES saved_views(workspace_id, id) ON DELETE CASCADE,
  FOREIGN KEY (workspace_id, label_id) REFERENCES labels(workspace_id, id)
);
CREATE TABLE saved_view_teams (
  workspace_id uuid NOT NULL,
  view_id uuid NOT NULL,
  team_id uuid NOT NULL,
  PRIMARY KEY (workspace_id, view_id, team_id),
  FOREIGN KEY (workspace_id, view_id) REFERENCES saved_views(workspace_id, id) ON DELETE CASCADE,
  FOREIGN KEY (workspace_id, team_id) REFERENCES teams(workspace_id, id)
);

CREATE TABLE triage_requests (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  workspace_id uuid NOT NULL,
  source_team_id uuid NOT NULL,
  target_team_id uuid NOT NULL,
  requester_id uuid NOT NULL,
  title text NOT NULL CHECK (length(trim(title)) > 0),
  description text NOT NULL DEFAULT '',
  state text NOT NULL DEFAULT 'pending' CHECK (state IN ('pending','accepted','declined')),
  decided_by uuid,
  decided_at timestamptz,
  decision_reason text,
  accepted_issue_id uuid,
  created_at timestamptz NOT NULL DEFAULT now(),
  FOREIGN KEY (workspace_id, source_team_id, requester_id) REFERENCES team_members(workspace_id, team_id, user_id),
  FOREIGN KEY (workspace_id, target_team_id) REFERENCES teams(workspace_id, id),
  FOREIGN KEY (workspace_id, decided_by) REFERENCES workspace_members(workspace_id, user_id),
  FOREIGN KEY (workspace_id, accepted_issue_id) REFERENCES issues(workspace_id, id),
  UNIQUE (accepted_issue_id),
  CHECK (source_team_id <> target_team_id),
  CHECK ((state = 'pending' AND decided_by IS NULL AND decided_at IS NULL AND accepted_issue_id IS NULL)
    OR (state = 'accepted' AND decided_by IS NOT NULL AND decided_at IS NOT NULL AND accepted_issue_id IS NOT NULL)
    OR (state = 'declined' AND decided_by IS NOT NULL AND decided_at IS NOT NULL AND accepted_issue_id IS NULL))
);

CREATE TABLE invitations (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  workspace_id uuid NOT NULL,
  invited_by uuid NOT NULL,
  email text NOT NULL CHECK (email = lower(email)),
  role text NOT NULL CHECK (role IN ('admin','member','viewer','guest')),
  token_hash text NOT NULL UNIQUE,
  expires_at timestamptz NOT NULL,
  accepted_at timestamptz,
  revoked_at timestamptz,
  FOREIGN KEY (workspace_id, invited_by) REFERENCES workspace_members(workspace_id, user_id)
);
CREATE TABLE guest_grants (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  workspace_id uuid NOT NULL,
  user_id uuid NOT NULL,
  team_id uuid NOT NULL,
  project_id uuid,
  FOREIGN KEY (workspace_id, user_id) REFERENCES workspace_members(workspace_id, user_id),
  FOREIGN KEY (workspace_id, team_id) REFERENCES teams(workspace_id, id),
  FOREIGN KEY (workspace_id, team_id, project_id) REFERENCES projects(workspace_id, team_id, id)
);

-- Identity authentication is delegated to an OIDC provider, not custom passwords.
CREATE TABLE oauth_connections (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL REFERENCES users(id),
  provider text NOT NULL,
  provider_subject text NOT NULL,
  scopes text[] NOT NULL DEFAULT '{}',
  secret_ref text, -- Reference into a secret manager; never raw provider tokens.
  created_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (provider, provider_subject),
  UNIQUE (user_id, provider)
);
CREATE TABLE sessions (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL REFERENCES users(id),
  token_hash text NOT NULL UNIQUE,
  user_agent text,
  expires_at timestamptz NOT NULL,
  revoked_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE workspace_security (
  workspace_id uuid PRIMARY KEY REFERENCES workspaces(id),
  require_mfa boolean NOT NULL DEFAULT false,
  session_timeout_minutes integer NOT NULL DEFAULT 1440 CHECK (session_timeout_minutes BETWEEN 5 AND 43200),
  allowed_email_domains text[] NOT NULL DEFAULT '{}'
);
CREATE TABLE api_keys (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  workspace_id uuid NOT NULL,
  owner_id uuid NOT NULL,
  name text NOT NULL,
  token_hash text NOT NULL UNIQUE,
  prefix text NOT NULL,
  scopes text[] NOT NULL CHECK (scopes <@ ARRAY['read','write','admin']::text[] AND cardinality(scopes) > 0),
  last_used_at timestamptz,
  expires_at timestamptz NOT NULL,
  revoked_at timestamptz,
  FOREIGN KEY (workspace_id, owner_id) REFERENCES workspace_members(workspace_id, user_id)
);

CREATE TABLE github_repositories (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  workspace_id uuid NOT NULL,
  team_id uuid NOT NULL,
  github_id bigint NOT NULL,
  full_name text NOT NULL,
  installation_secret_ref text NOT NULL,
  synced_at timestamptz,
  UNIQUE (workspace_id, id),
  UNIQUE (workspace_id, github_id),
  FOREIGN KEY (workspace_id, team_id) REFERENCES teams(workspace_id, id)
);
CREATE TABLE github_issue_links (
  workspace_id uuid NOT NULL,
  issue_id uuid NOT NULL,
  repository_id uuid NOT NULL,
  github_number integer NOT NULL CHECK (github_number > 0),
  github_node_id text NOT NULL,
  url text NOT NULL,
  last_synced_at timestamptz,
  PRIMARY KEY (workspace_id, issue_id),
  UNIQUE (workspace_id, repository_id, github_number),
  FOREIGN KEY (workspace_id, issue_id) REFERENCES issues(workspace_id, id),
  FOREIGN KEY (workspace_id, repository_id) REFERENCES github_repositories(workspace_id, id)
);

-- Current cycle status/progress and Insights counts are derived, not stored.
-- Historical burndown uses issue_activity and cycle assignment/status changes.

