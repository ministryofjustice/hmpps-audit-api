CREATE TABLE audit_user_id
(
    id                 SERIAL PRIMARY KEY,
    audit_user_id      BIGINT       NOT NULL,
    user_id            VARCHAR(255) NOT NULL,
    active             BOOLEAN      NOT NULL,
    creation_time      TIMESTAMP    NOT NULL,
    last_modified_time TIMESTAMP
);

CREATE TABLE audit_email_address
(
    id                 SERIAL PRIMARY KEY,
    audit_user_id      BIGINT       NOT NULL,
    email_address      VARCHAR(255) NOT NULL,
    active             BOOLEAN      NOT NULL,
    creation_time      TIMESTAMP    NOT NULL,
    last_modified_time TIMESTAMP
);

CREATE TABLE audit_username
(
    id                 SERIAL PRIMARY KEY,
    audit_user_id      BIGINT       NOT NULL,
    username           VARCHAR(255) NOT NULL,
    active             BOOLEAN      NOT NULL,
    creation_time      TIMESTAMP    NOT NULL,
    last_modified_time TIMESTAMP
);


