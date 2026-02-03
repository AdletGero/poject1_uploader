create table upload_outbox (
                               id uuid primary key,
                               upload_id uuid not null,
                               status varchar(16) not null,
                               created_at timestamptz not null,
                               sent_at timestamptz
);

create index ix_upload_outbox_status
    on upload_outbox (status, created_at);