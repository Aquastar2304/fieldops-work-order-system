// Package report builds the overdue work order report that the maintenance
// supervisors pull each morning.
package report

import (
	"context"
	"encoding/csv"
	"errors"
	"fmt"
	"io"
	"strconv"
	"time"
)

// ErrInvalidWindow is returned when the caller asks for a non-positive day window.
var ErrInvalidWindow = errors.New("the day window must be greater than zero")

// OverdueWorkOrder is one row of the report.
type OverdueWorkOrder struct {
	Reference  string    `json:"reference"`
	Title      string    `json:"title"`
	Status     string    `json:"status"`
	Priority   string    `json:"priority"`
	AssetTag   string    `json:"assetTag"`
	SiteName   string    `json:"site"`
	Technician string    `json:"technician,omitempty"`
	OpenedAt   time.Time `json:"openedAt"`
	DaysOpen   int       `json:"daysOpen"`
}

// Store is the read side this package needs. Keeping it an interface lets the
// report logic be tested without a database.
type Store interface {
	FindOverdue(ctx context.Context, threshold time.Time) ([]OverdueWorkOrder, error)
}

// Service turns a day window into a finished report.
type Service struct {
	store Store
	now   func() time.Time
}

// NewService wires a Service. Passing now explicitly keeps DaysOpen testable.
func NewService(store Store, now func() time.Time) *Service {
	if now == nil {
		now = time.Now
	}
	return &Service{store: store, now: now}
}

// Overdue returns every work order that is still not closed and was opened more
// than days ago, oldest first.
func (s *Service) Overdue(ctx context.Context, days int) ([]OverdueWorkOrder, error) {
	if days <= 0 {
		return nil, fmt.Errorf("%w: got %d", ErrInvalidWindow, days)
	}

	now := s.now()
	rows, err := s.store.FindOverdue(ctx, now.AddDate(0, 0, -days))
	if err != nil {
		return nil, fmt.Errorf("querying overdue work orders: %w", err)
	}

	// Never hand back a nil slice: it would marshal to JSON null rather than [].
	report := make([]OverdueWorkOrder, 0, len(rows))
	for _, row := range rows {
		row.DaysOpen = int(now.Sub(row.OpenedAt).Hours() / 24)
		report = append(report, row)
	}
	return report, nil
}

var csvHeader = []string{
	"reference", "title", "status", "priority",
	"asset_tag", "site", "technician", "opened_at", "days_open",
}

// WriteCSV renders the report as CSV, header first, even when there are no rows.
func WriteCSV(w io.Writer, rows []OverdueWorkOrder) error {
	writer := csv.NewWriter(w)

	if err := writer.Write(csvHeader); err != nil {
		return fmt.Errorf("writing CSV header: %w", err)
	}
	for _, row := range rows {
		record := []string{
			row.Reference,
			row.Title,
			row.Status,
			row.Priority,
			row.AssetTag,
			row.SiteName,
			row.Technician,
			row.OpenedAt.UTC().Format(time.RFC3339),
			strconv.Itoa(row.DaysOpen),
		}
		if err := writer.Write(record); err != nil {
			return fmt.Errorf("writing CSV row %s: %w", row.Reference, err)
		}
	}

	writer.Flush()
	return writer.Error()
}
