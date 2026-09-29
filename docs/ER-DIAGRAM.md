# FieldOps — Entity relationship diagram

The schema is created by `backend/src/main/resources/db/migration/V1__init_schema.sql`.

```mermaid
erDiagram
    SITE ||--o{ ASSET : "houses"
    ASSET ||--o{ WORK_ORDER : "is serviced by"
    TECHNICIAN |o--o{ WORK_ORDER : "is assigned"
    WORK_ORDER ||--o{ WORK_ORDER_PART : "consumes"
    PART ||--o{ WORK_ORDER_PART : "is consumed as"

    SITE {
        bigserial id PK
        varchar   code UK "unique site code"
        varchar   name
        varchar   location
    }

    ASSET {
        bigserial id PK
        varchar   tag UK "unique equipment tag"
        varchar   name
        varchar   manufacturer
        varchar   model
        bigint    site_id FK
    }

    TECHNICIAN {
        bigserial id PK
        varchar   employee_no UK
        varchar   full_name
        varchar   email
        varchar   specialty
    }

    PART {
        bigserial id PK
        varchar   sku UK
        varchar   name
        integer   quantity_in_stock "check >= 0"
        bigint    version "optimistic lock"
    }

    WORK_ORDER {
        bigserial   id PK
        varchar     reference UK "WO-000001"
        varchar     title
        text        description
        varchar     status "OPEN|ASSIGNED|IN_PROGRESS|CLOSED"
        varchar     priority "LOW|MEDIUM|HIGH|CRITICAL"
        bigint      asset_id FK
        bigint      technician_id FK "nullable"
        timestamptz opened_at
        timestamptz closed_at "nullable"
        bigint      version "optimistic lock"
    }

    WORK_ORDER_PART {
        bigserial id PK
        bigint    work_order_id FK
        bigint    part_id FK
        integer   quantity "check > 0"
    }

    APP_USER {
        bigserial id PK
        varchar   username UK
        varchar   password_hash "bcrypt"
        varchar   role "ADMIN|SUPERVISOR|TECHNICIAN"
        boolean   enabled
    }
```

## Relationship notes

| Relationship | Cardinality | Why |
|---|---|---|
| Site → Asset | one-to-many, mandatory | Every asset stands somewhere; an asset cannot exist without a site. |
| Asset → WorkOrder | one-to-many, mandatory | A work order is always raised against a specific piece of equipment. |
| Technician → WorkOrder | zero-or-one to many | A work order starts life unassigned, so `technician_id` is nullable. |
| WorkOrder ↔ Part | many-to-many via `WORK_ORDER_PART` | The join carries a quantity, so it is modelled as its own entity rather than a plain join table. |

## Constraints worth calling out

- `uq_work_order_part (work_order_id, part_id)` — a work order holds at most one
  line per part; consuming the same part twice merges into that line.
- `chk_part_stock_not_negative` — the store room quantity can never go below
  zero, independently of the service-layer check.
- `chk_closed_requires_technician` — `status <> 'CLOSED' OR technician_id IS NOT
  NULL`. The business rule restated where the data lives.
- `chk_work_order_part_quantity` — a consumed quantity is always positive.

## Indexes

| Index | Column(s) | Serves |
|---|---|---|
| `idx_work_order_status` | `status` | the list screen's status filter |
| `idx_work_order_opened_at` | `opened_at` | default sort and the overdue report |
| `idx_work_order_asset` | `asset_id` | asset history |
| `idx_work_order_technician` | `technician_id` | a technician's queue |
| `idx_asset_site` | `site_id` | assets by site |
| `idx_work_order_part_part` | `part_id` | where a given part was consumed |
