create table uploads (
                         id uuid primary key,
                         client_id varchar(64) not null,
                         idempotency_key varchar(128) not null,

                         status varchar(24) not null,
                         original_filename varchar(512),
                         content_type varchar(128),
                         size_bytes bigint not null,

                         temp_path varchar(1024) not null,
                         storage_key varchar(1024),
                         error_message varchar(2048),

                         created_at timestamptz not null,
                         updated_at timestamptz not null
);

create unique index ux_uploads_client_idem
    on uploads (client_id, idempotency_key);

create index ix_uploads_status
    on uploads (status);
