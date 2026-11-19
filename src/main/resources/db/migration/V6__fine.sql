-- Lab 12, individual variant 6: fines for books returned late.
-- The amount is days_overdue * the daily rate, but never more than the maximum (app.fines.* in
-- application.properties); FineService computes it, the table only refuses nonsense.
create table fine (
    id           bigserial   primary key,
    book_id      bigint      not null references book (id) on delete cascade,
    reader       varchar(60) not null,
    days_overdue integer     not null check (days_overdue > 0),
    amount       integer     not null check (amount > 0),     -- tenge
    paid         boolean     not null default false
);

create index ix_fine_unpaid_by_reader on fine (reader) where not paid;
