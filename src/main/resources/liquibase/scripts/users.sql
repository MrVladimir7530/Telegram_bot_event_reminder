--liquibase formatted sql

--changeset volkov:create_table1
CREATE TABLE IF NOT EXISTS users_data_table(
    chat_id bigint NOT NULL PRIMARY KEY,
    first_name character varying(255),
    last_name character varying(255),
    registered_at timestamp without time zone,
    username character varying(255)
);

--changeset volkov:create_table2
CREATE TABLE IF NOT EXISTS user_reminder(
    id bigint NOT NULL PRIMARY KEY,
    data_reminder timestamp without time zone,
    message_reminder character varying(255),
    user_chat_id bigint NOT NULL,
    foreign key (user_chat_id) REFERENCES users_data_table (chat_id)
);