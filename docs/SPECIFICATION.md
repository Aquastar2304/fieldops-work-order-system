# FieldOps — Specification

## 1. Problem statement

Field maintenance teams in the energy sector look after equipment spread across
remote sites. When a pump starts vibrating or a separator reports an erratic
level, someone raises a work order, a supervisor assigns a technician, and the
technician draws spare parts from a store room to carry out the repair.

In many organisations that whole flow lives in spreadsheets and phone calls.
The consequences are predictable:

- Nobody can answer "what is still open on Platform 3?" without asking around.
- Parts are consumed without the inventory count being updated, so the store
  room shows stock that is not physically there.
- Work orders get marked complete before anyone was actually assigned to them,
  so the audit trail does not survive scrutiny.
- There is no reliable list of jobs that have been sitting open too long.

FieldOps replaces that with a single system of record for work orders, the
assets they are raised against, the technicians who carry them out, and the
parts they consume.

## 2. Users and roles

| Role | Who they are | What they can do |
|---|---|---|
| `ADMIN` | Systems administrator | Everything, including deleting records and maintaining users |
| `SUPERVISOR` | Maintenance planner or shift supervisor | Raise work orders, assign technicians, maintain sites, assets, technicians and parts |
| `TECHNICIAN` | Field technician | View work orders, move their own work along the lifecycle, record consumed parts |

Authentication is a bearer token obtained from `POST /api/auth/login`. The token
carries the username and a single role claim, and every other endpoint requires
it.

## 3. Domain model

- **Site** — a physical location such as a gathering station or a platform.
- **Asset** — a serviceable piece of equipment. Every asset belongs to one site.
- **Technician** — a person who carries out maintenance.
- **Part** — a stock-keeping unit held in the store room, with a current
  quantity on hand.
- **Work order** — a unit of maintenance work raised against one asset,
  optionally assigned to one technician, with a status, a priority and a list of
  consumed parts.
- **WorkOrderPart** — the join between a work order and a part, recording how
  many units that work order consumed.

## 4. Work order lifecycle

```
            assign technician
     OPEN ────────────────────► ASSIGNED
       ▲                          │   ▲
       │      unassign            │   │ reassign
       └──────────────────────────┘   │
                                  │   │
                     start work    │   │
                                  ▼   │
                            IN_PROGRESS
                                  │
                      complete    │
                                  ▼
                               CLOSED   (terminal)
```

The moves permitted are exactly:

| From | To |
|---|---|
| `OPEN` | `ASSIGNED` |
| `ASSIGNED` | `IN_PROGRESS`, `OPEN` |
| `IN_PROGRESS` | `CLOSED`, `ASSIGNED` |
| `CLOSED` | — nothing, the status is terminal |

Anything else is rejected with `409 Conflict`.

## 5. Functional requirements

### FR-1 Reference data
The system maintains sites, assets, technicians and parts through full CRUD.
Natural keys (site code, asset tag, employee number, part SKU) are unique, and
an attempt to reuse one is rejected.

### FR-2 Raising work orders
A work order is raised against exactly one existing asset. A title and a
priority are required. It may optionally be assigned to a technician at the
point of creation, in which case it is created as `ASSIGNED` rather than `OPEN`.
Each work order receives a unique human-readable reference (`WO-000001`).

### FR-3 Assignment
A supervisor or admin may assign a technician to any work order that is not
closed. Assigning a technician to an `OPEN` work order also advances it to
`ASSIGNED`, because those two facts are the same event.

### FR-4 Status transitions
A work order may only move along the lifecycle in section 4.

### FR-5 Closing requires a technician
A work order cannot be moved to `CLOSED` unless a technician is assigned. This
is enforced in the domain model and again by a check constraint on the table,
so the rule holds even if a future caller bypasses the service.

### FR-6 Consuming parts
Recording a part against a work order reduces the store room quantity by the
same amount, in a single transaction. If the store room holds fewer units than
requested, the whole operation fails and neither the part line nor the stock
level changes. Consuming a part a second time merges into the existing line
rather than creating a duplicate.

### FR-7 Listing and filtering
Work orders can be listed with a status filter and pagination, sorted by opened
date descending by default.

### FR-8 Overdue reporting
A separate Go service reports work orders that are still not closed and were
opened more than N days ago, as JSON or CSV.

## 6. Non-functional requirements

### NFR-1 Concurrency
Work orders carry an optimistic lock (`@Version`), so two supervisors editing
the same record concurrently produce a `409` rather than a silent overwrite.
Stock decrements take a pessimistic write lock on the part row, so two
concurrent consumptions cannot both read the same pre-decrement quantity.

### NFR-2 Consistent errors
Every failure returns the same JSON shape with a timestamp, status, message,
request path and, for validation failures, a list of per-field violations.
Internal exception detail is logged rather than returned.

### NFR-3 Schema management
The schema is owned by Flyway migrations, and Hibernate is set to `validate`,
never to generate. This means the schema in source control is the schema that
runs.

### NFR-4 API documentation
The API is described by OpenAPI 3 and browsable through Swagger UI.

### NFR-5 Testability
Business rules are unit tested without a database. The repository layer and the
migrations are tested against a real PostgreSQL through Testcontainers.

### NFR-6 Reproducible builds
Every service builds and runs from `docker compose up`, and the CI pipeline runs
the same test suites on every push.

## 7. Out of scope for this version

See the roadmap section of the README. In short: attachments, scheduled
preventive maintenance, mobile offline support, purchase orders and email
notification are not implemented.
