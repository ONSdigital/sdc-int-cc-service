CREATE TABLE user_audit (
    id                UUID PRIMARY KEY,
    ccuser_id         UUID REFERENCES ccuser NOT NULL,
    target_user_id    UUID REFERENCES ccuser,
    target_role_id    UUID REFERENCES role,
    created_date_time TIMESTAMP NOT NULL,
    audit_type        TEXT NOT NULL,
    audit_sub_type    TEXT,
    audit_value       TEXT
);