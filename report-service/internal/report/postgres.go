package report

import (
	"context"
	"database/sql"
	"fmt"
	"time"
)

// PostgresStore reads the report straight from the work order tables the Spring
// Boot service owns. It only ever reads.
type PostgresStore struct {
	db *sql.DB
}

func NewPostgresStore(db *sql.DB) *PostgresStore {
	return &PostgresStore{db: db}
}

const overdueQuery = `
select w.reference,
       w.title,
       w.status,
       w.priority,
       a.tag,
       s.name,
       coalesce(t.full_name, ''),
       w.opened_at
from work_order w
         join asset a on a.id = w.asset_id
         join site s on s.id = a.site_id
         left join technician t on t.id = w.technician_id
where w.status <> 'CLOSED'
  and w.opened_at < $1
order by w.opened_at asc`

// FindOverdue returns unclosed work orders opened before threshold, oldest first.
func (s *PostgresStore) FindOverdue(ctx context.Context, threshold time.Time) ([]OverdueWorkOrder, error) {
	rows, err := s.db.QueryContext(ctx, overdueQuery, threshold)
	if err != nil {
		return nil, fmt.Errorf("executing overdue query: %w", err)
	}
	defer rows.Close()

	var result []OverdueWorkOrder
	for rows.Next() {
		var wo OverdueWorkOrder
		if err := rows.Scan(
			&wo.Reference,
			&wo.Title,
			&wo.Status,
			&wo.Priority,
			&wo.AssetTag,
			&wo.SiteName,
			&wo.Technician,
			&wo.OpenedAt,
		); err != nil {
			return nil, fmt.Errorf("scanning overdue row: %w", err)
		}
		result = append(result, wo)
	}
	if err := rows.Err(); err != nil {
		return nil, fmt.Errorf("iterating overdue rows: %w", err)
	}
	return result, nil
}
