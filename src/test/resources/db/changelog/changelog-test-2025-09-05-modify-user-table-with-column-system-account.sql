-- liquibase formatted sql

--changeset lulippe:1
--comment PMB-15- modify user table by adding a new columnn system account

ALTER TABLE app_user ADD COLUMN system_account BOOLEAN NOT NULL DEFAULT false;

--changeset lulippe:2
INSERT INTO app_user (username, email, password, role, account, system_account)
VALUES ('PLATFORM', 'platform@paymybuddy.com', 'changeIt', 'ROLE_SYSTEM', 0, true);

