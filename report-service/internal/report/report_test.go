package report

import (
	"bytes"
	"context"
	"encoding/csv"
	"errors"
	"strings"
	"testing"
	"time"
)

// fixedNow pins "today" so DaysOpen is deterministic.
var fixedNow = time.Date(2026, 3, 20, 12, 0, 0, 0, time.UTC)

type fakeStore struct {
	rows         []OverdueWorkOrder
	err          error
	gotThreshold time.Time
	callCount    int
}

func (f *fakeStore) FindOverdue(_ context.Context, threshold time.Time) ([]OverdueWorkOrder, error) {
	f.callCount++
	f.gotThreshold = threshold
	return f.rows, f.err
}

func sampleRows() []OverdueWorkOrder {
	return []OverdueWorkOrder{
		{
			Reference:  "WO-000003",
			Title:      "Level transmitter reading erratic",
			Status:     "OPEN",
			Priority:   "MEDIUM",
			AssetTag:   "SEP-003",
			SiteName:   "Permian West Battery",
			Technician: "",
			OpenedAt:   fixedNow.AddDate(0, 0, -18),
		},
		{
			Reference:  "WO-000001",
			Title:      "Excessive vibration on start-up",
			Status:     "IN_PROGRESS",
			Priority:   "HIGH",
			AssetTag:   "PMP-001",
			SiteName:   "Bakken North Gathering",
			Technician: "Dana Okoro",
			OpenedAt:   fixedNow.AddDate(0, 0, -11),
		},
	}
}

func TestOverdueDerivesThresholdFromDays(t *testing.T) {
	store := &fakeStore{rows: sampleRows()}
	svc := NewService(store, func() time.Time { return fixedNow })

	if _, err := svc.Overdue(context.Background(), 7); err != nil {
		t.Fatalf("unexpected error: %v", err)
	}

	want := fixedNow.AddDate(0, 0, -7)
	if !store.gotThreshold.Equal(want) {
		t.Errorf("threshold = %v, want %v", store.gotThreshold, want)
	}
}

func TestOverdueComputesDaysOpen(t *testing.T) {
	store := &fakeStore{rows: sampleRows()}
	svc := NewService(store, func() time.Time { return fixedNow })

	rows, err := svc.Overdue(context.Background(), 7)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if len(rows) != 2 {
		t.Fatalf("got %d rows, want 2", len(rows))
	}
	if rows[0].DaysOpen != 18 {
		t.Errorf("rows[0].DaysOpen = %d, want 18", rows[0].DaysOpen)
	}
	if rows[1].DaysOpen != 11 {
		t.Errorf("rows[1].DaysOpen = %d, want 11", rows[1].DaysOpen)
	}
}

func TestOverdueRejectsANonPositiveWindow(t *testing.T) {
	store := &fakeStore{rows: sampleRows()}
	svc := NewService(store, func() time.Time { return fixedNow })

	for _, days := range []int{0, -1, -30} {
		if _, err := svc.Overdue(context.Background(), days); !errors.Is(err, ErrInvalidWindow) {
			t.Errorf("days=%d: err = %v, want ErrInvalidWindow", days, err)
		}
	}
	if store.callCount != 0 {
		t.Errorf("store was queried %d times, want 0", store.callCount)
	}
}

func TestOverduePropagatesStoreFailure(t *testing.T) {
	boom := errors.New("connection refused")
	svc := NewService(&fakeStore{err: boom}, func() time.Time { return fixedNow })

	if _, err := svc.Overdue(context.Background(), 7); !errors.Is(err, boom) {
		t.Errorf("err = %v, want it to wrap %v", err, boom)
	}
}

func TestOverdueReturnsEmptySliceNotNil(t *testing.T) {
	svc := NewService(&fakeStore{rows: nil}, func() time.Time { return fixedNow })

	rows, err := svc.Overdue(context.Background(), 7)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	// A nil slice marshals to JSON null; the API should return [].
	if rows == nil {
		t.Error("rows is nil, want an empty slice so it marshals as []")
	}
}

func TestWriteCSVEmitsAHeaderAndOneRowPerWorkOrder(t *testing.T) {
	svc := NewService(&fakeStore{rows: sampleRows()}, func() time.Time { return fixedNow })
	rows, _ := svc.Overdue(context.Background(), 7)

	var buf bytes.Buffer
	if err := WriteCSV(&buf, rows); err != nil {
		t.Fatalf("unexpected error: %v", err)
	}

	records, err := csv.NewReader(strings.NewReader(buf.String())).ReadAll()
	if err != nil {
		t.Fatalf("output is not valid CSV: %v", err)
	}
	if len(records) != 3 {
		t.Fatalf("got %d records (including header), want 3", len(records))
	}

	wantHeader := []string{
		"reference", "title", "status", "priority",
		"asset_tag", "site", "technician", "opened_at", "days_open",
	}
	for i, h := range wantHeader {
		if records[0][i] != h {
			t.Errorf("header[%d] = %q, want %q", i, records[0][i], h)
		}
	}

	if records[1][0] != "WO-000003" {
		t.Errorf("first data row reference = %q, want WO-000003", records[1][0])
	}
	if records[1][8] != "18" {
		t.Errorf("first data row days_open = %q, want 18", records[1][8])
	}
}

func TestWriteCSVLeavesAnUnassignedTechnicianBlank(t *testing.T) {
	svc := NewService(&fakeStore{rows: sampleRows()}, func() time.Time { return fixedNow })
	rows, _ := svc.Overdue(context.Background(), 7)

	var buf bytes.Buffer
	if err := WriteCSV(&buf, rows); err != nil {
		t.Fatalf("unexpected error: %v", err)
	}

	records, _ := csv.NewReader(strings.NewReader(buf.String())).ReadAll()
	if records[1][6] != "" {
		t.Errorf("technician = %q, want an empty field", records[1][6])
	}
}

func TestWriteCSVWithNoRowsStillEmitsTheHeader(t *testing.T) {
	var buf bytes.Buffer
	if err := WriteCSV(&buf, []OverdueWorkOrder{}); err != nil {
		t.Fatalf("unexpected error: %v", err)
	}

	records, err := csv.NewReader(strings.NewReader(buf.String())).ReadAll()
	if err != nil {
		t.Fatalf("output is not valid CSV: %v", err)
	}
	if len(records) != 1 {
		t.Errorf("got %d records, want just the header", len(records))
	}
}

func TestWriteCSVQuotesFieldsContainingCommas(t *testing.T) {
	rows := []OverdueWorkOrder{{
		Reference: "WO-000009",
		Title:     "Replace seal, gasket and bearing",
		Status:    "OPEN",
		Priority:  "LOW",
		AssetTag:  "PMP-001",
		SiteName:  "Bakken North Gathering",
		OpenedAt:  fixedNow.AddDate(0, 0, -9),
		DaysOpen:  9,
	}}

	var buf bytes.Buffer
	if err := WriteCSV(&buf, rows); err != nil {
		t.Fatalf("unexpected error: %v", err)
	}

	records, err := csv.NewReader(strings.NewReader(buf.String())).ReadAll()
	if err != nil {
		t.Fatalf("output is not valid CSV: %v", err)
	}
	if records[1][1] != "Replace seal, gasket and bearing" {
		t.Errorf("title round-tripped as %q", records[1][1])
	}
}
