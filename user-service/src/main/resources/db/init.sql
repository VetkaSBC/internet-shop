DROP TABLE IF EXISTS user_role CASCADE;
DROP TABLE IF EXISTS roles CASCADE;
DROP SEQUENCE IF EXISTS roles_id_seq CASCADE;

CREATE TABLE roles (
                       id BIGSERIAL PRIMARY KEY,
                       role_name VARCHAR(50) NOT NULL UNIQUE
);

INSERT INTO roles (role_name) VALUES ('USER');
INSERT INTO roles (role_name) VALUES ('ADMIN');