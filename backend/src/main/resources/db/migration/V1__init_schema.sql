-- Core schema for the field maintenance work order system.

create table site (
    id       bigserial primary key,
    code     varchar(40)  not null unique,
    name     varchar(120) not null,
    location varchar(200)
);

create table asset (
    id           bigserial primary key,
    tag          varchar(40)  not null unique,
    name         varchar(120) not null,
    manufacturer varchar(80),
    model        varchar(80),
    site_id      bigint       not null references site (id)
);

create index idx_asset_site on asset (site_id);

create table technician (
    id          bigserial primary key,
    employee_no varchar(40)  not null unique,
    full_name   varchar(120) not null,
    email       varchar(120),
    specialty   varchar(80)
);

create table part (
    id                bigserial primary key,
    sku               varchar(40)  not null unique,
    name              varchar(120) not null,
    quantity_in_stock integer      not null,
    version           bigint,
    constraint chk_part_stock_not_negative check (quantity_in_stock >= 0)
);

create table work_order (
    id            bigserial primary key,
    reference     varchar(40)  not null unique,
    title         varchar(200) not null,
    description   text,
    status        varchar(20)  not null,
    priority      varchar(20)  not null,
    asset_id      bigint       not null references asset (id),
    technician_id bigint       references technician (id),
    opened_at     timestamptz  not null,
    closed_at     timestamptz,
    version       bigint,
    constraint chk_work_order_status
        check (status in ('OPEN', 'ASSIGNED', 'IN_PROGRESS', 'CLOSED')),
    constraint chk_work_order_priority
        check (priority in ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    -- The rule the service enforces, restated where the data lives.
    constraint chk_closed_requires_technician
        check (status <> 'CLOSED' or technician_id is not null)
);

create index idx_work_order_status on work_order (status);
create index idx_work_order_asset on work_order (asset_id);
create index idx_work_order_technician on work_order (technician_id);
create index idx_work_order_opened_at on work_order (opened_at);

create table work_order_part (
    id            bigserial primary key,
    work_order_id bigint  not null references work_order (id) on delete cascade,
    part_id       bigint  not null references part (id),
    quantity      integer not null,
    constraint uq_work_order_part unique (work_order_id, part_id),
    constraint chk_work_order_part_quantity check (quantity > 0)
);

create index idx_work_order_part_part on work_order_part (part_id);

create table app_user (
    id            bigserial primary key,
    username      varchar(60)  not null unique,
    password_hash varchar(100) not null,
    role          varchar(20)  not null,
    enabled       boolean      not null default true,
    constraint chk_app_user_role check (role in ('ADMIN', 'SUPERVISOR', 'TECHNICIAN'))
);

-- Human readable work order references (WO-000001, WO-000002, ...).
create sequence work_order_reference_seq start with 1 increment by 1;
