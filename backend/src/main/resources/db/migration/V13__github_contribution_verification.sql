ALTER TABLE tasks
    ADD COLUMN project_id BIGINT,
    ADD CONSTRAINT fk_tasks_project FOREIGN KEY (project_id)
        REFERENCES projects (id) ON DELETE SET NULL;

CREATE INDEX ix_tasks_project
    ON tasks (project_id) WHERE project_id IS NOT NULL;

ALTER TABLE github_contributions
    ADD COLUMN verified BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN verified_by BIGINT,
    ADD COLUMN verified_at TIMESTAMPTZ,
    ADD CONSTRAINT fk_github_contributions_verified_by FOREIGN KEY (verified_by)
        REFERENCES users (id) ON DELETE RESTRICT,
    ADD CONSTRAINT ck_github_contributions_verification_state CHECK (
        (verified = FALSE AND verified_by IS NULL AND verified_at IS NULL)
        OR (verified = TRUE AND verified_by IS NOT NULL AND verified_at IS NOT NULL)
    );

CREATE INDEX ix_github_contributions_verified_by
    ON github_contributions (verified_by) WHERE verified_by IS NOT NULL;
