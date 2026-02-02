create table outbox (
                        id uuid primary key,
                        aggregate_id uuid not null,
                        event_type varchar(64) not null,
                        payload jsonb not null,

                        status varchar(16) not null,
                        attempts int not null default 0,
                        last_error varchar(2048),

                        created_at timestamptz not null,
                        updated_at timestamptz not null
);

create index ix_outbox_status_created
    on outbox (status, created_at);
