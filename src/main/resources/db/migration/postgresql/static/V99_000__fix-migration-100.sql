DO
$$

    DECLARE
        executed BOOLEAN;
    BEGIN
        SELECT true INTO executed FROM flyway_schema_history where version = '10.001' or version = '100.000';

        IF coalesce(executed, false) is false THEN

            create table IF NOT EXISTS sessions_info
            (
                id                  varchar(255) not null
                    constraint pk_sessions_info
                        primary key,
                started             timestamp,
                finished            timestamp,
                duration            bigint       not null,
                execution_status    integer,
                chain_id            varchar(255),
                chain_name          varchar(255),
                engine_address      varchar(255),
                logging_level       varchar(255),
                snapshot_name       varchar(255),
                correlation_id      varchar(255),
                original_session_id varchar(255)
                    constraint fk_sessions_info_on_originalsession
                        references sessions_info
                        on delete cascade,
                domain              varchar(255) default NULL::character varying
            );

            create index IF NOT EXISTS idx_sessions_info_started
                on sessions_info (started);

            create index IF NOT EXISTS idx_sessions_info_original_session_id
                on sessions_info (original_session_id);

            create index IF NOT EXISTS idx_sessions_info_chain_id_execution_status
                on sessions_info (chain_id, execution_status);

            create table IF NOT EXISTS checkpoints
            (
                id                    varchar(255) not null
                    constraint pk_checkpoints
                        primary key,
                session_id            varchar(255)
                    constraint fk_checkpoints_on_session
                        references sessions_info
                        on delete cascade,
                checkpoint_element_id varchar(255),
                headers               text,
                body                  oid,
                timestamp             timestamp,
                context_data          text
            );

            create index IF NOT EXISTS sessionid_checkpoints
                on checkpoints (session_id);

            create index IF NOT EXISTS idx_checkpoints_session_id_checkpoint_element_id
                on checkpoints (session_id, checkpoint_element_id);

            create table IF NOT EXISTS properties
            (
                id            varchar(255) not null
                    constraint pk_properties
                        primary key,
                name          varchar(255),
                type          varchar(255),
                value         oid,
                checkpoint_id varchar(255)
                    constraint fk_checkpoints_on_chain
                        references checkpoints
                        on delete cascade
            );

            insert into flyway_schema_history(installed_rank, version, description, type, script, checksum,
                                              installed_by, execution_time, success)
            values ((select coalesce(max(installed_rank), 0) + 2 from flyway_schema_history),
                    '100.000',
                    'init-static',
                    'SQL',
                    'V100_000__init-static.sql',
                    -1873308953,
                    CURRENT_USER,
                    7,
                    true);
        END IF;

    END
$$;
