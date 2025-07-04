-- liquibase formatted sql

--changeset lulippe:test-1
--comment PMB-3-Create user and transaction tables for test schema
CREATE TABLE IF NOT EXISTS app_user
(
    user_id  NUMERIC      NOT NULL PRIMARY KEY,
    username VARCHAR(50)  NOT NULL UNIQUE,
    email    VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role     VARCHAR(20)  NOT NULL DEFAULT 'ROLE_USER',
    account  NUMERIC(15, 2)        DEFAULT 0
);

CREATE TABLE IF NOT EXISTS user_friend
(
    user_id   INTEGER NOT NULL,
    friend_id INTEGER NOT NULL,
    CONSTRAINT fk_user_friend_user FOREIGN KEY (user_id) REFERENCES app_user (user_id),
    CONSTRAINT fk_user_friend_friend FOREIGN KEY (friend_id) REFERENCES app_user (user_id),
    CONSTRAINT uc_user_friend_unique UNIQUE (user_id, friend_id)
);

CREATE TABLE IF NOT EXISTS transactions
(
    transaction_id INTEGER        NOT NULL PRIMARY KEY,
    sender_id      INTEGER        NOT NULL,
    receiver_id    INTEGER        NOT NULL,
    description    VARCHAR(255),
    amount         NUMERIC(15, 2) NOT NULL,
    executed_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_transactions_sender FOREIGN KEY (sender_id) REFERENCES app_user (user_id),
    CONSTRAINT fk_transactions_receiver FOREIGN KEY (receiver_id) REFERENCES app_user (user_id)
);