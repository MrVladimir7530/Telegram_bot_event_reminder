--liquibase formatted sql

--changeset volkov:create_table1
CREATE TABLE IF NOT EXISTS public.users_data_table
(
    chat_id bigint NOT NULL,
    first_name character varying(255) COLLATE pg_catalog."default",
    last_name character varying(255) COLLATE pg_catalog."default",
    registered_at timestamp without time zone,
    username character varying(255) COLLATE pg_catalog."default",
    CONSTRAINT users_data_table_pkey PRIMARY KEY (chat_id)
    )

    TABLESPACE pg_default;



CREATE TABLE IF NOT EXISTS public.user_reminder
(
    id bigint NOT NULL,
    data_reminder timestamp without time zone,
    message_reminder character varying(255) COLLATE pg_catalog."default",
    user_chat_id bigint,
    CONSTRAINT user_reminder_pkey PRIMARY KEY (id),
    CONSTRAINT fkj574uebbgrs5xpffugxbt8qu FOREIGN KEY (user_chat_id)
    REFERENCES public.users_data_table (chat_id) MATCH SIMPLE
    ON UPDATE NO ACTION
    ON DELETE NO ACTION
    )

    TABLESPACE pg_default;


ALTER TABLE IF EXISTS public.users_data_table
    OWNER to "AccountTest";

ALTER TABLE IF EXISTS public.user_reminder
    OWNER to "AccountTest";