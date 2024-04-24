CREATE TABLE auth_user_uuid
(
    id                 SERIAL PRIMARY KEY,
    audit_user_id      UUID      NOT NULL,
    user_uuid          UUID      NOT NULL,
    active             BOOLEAN   NOT NULL,
    creation_time      TIMESTAMP NOT NULL,
    last_modified_time TIMESTAMP,
    FOREIGN KEY (audit_user_id) REFERENCES audit_user (id)
);
