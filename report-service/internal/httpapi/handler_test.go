package httpapi

import (
	"context"
	"encoding/csv"
	"encoding/json"
	"errors"
	"io"
	"log/slog"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
	"time"

	"github.com/Aquastar2304/fieldops-work-order-system/report-service/internal/report"
)

var fixedNow = time.Date(2026, 3, 20, 12, 0, 0, 0, time.UTC)

type stubStore struct {
	rows []report.OverdueWorkOrder
	err  error
}

func (s stubStore) FindOverdue(_ context.Context, _ time.Time) ([]report.OverdueWorkOrder, error) {
	return s.rows, s.err
}

func newTestHandler(store report.Store) http.Handler {
	svc := report.NewService(store, func() time.Time { return fixedNow })
	// Discard log output so a failing-path test does not spam the test run.
	quiet := slog.New(slog.NewTextHandler(io.Discard, nil))
	return NewHandler(svc, 7, quiet).Routes()
}

func sampleStore() stubStore {
	return stubStore{rows: []report.OverdueWorkOrder{{
		Reference:  "WO-000003",
		Title:      "Level transmitter reading erratic",
		Status:     "OPEN",
		Priority:   "MEDIUM",
		AssetTag:   "SEP-003",
		SiteName:   "Permian West Battery",
		Technician: "",
		OpenedAt:   fixedNow.AddDate(0, 0, -18),
	}}}
}

func do(t *testing.T, h http.Handler, target string) *httptest.ResponseRecorder {
	t.Helper()
	rec := httptest.NewRecorder()
	h.ServeHTTP(rec, httptest.NewRequest(http.MethodGet, target, nil))
	return rec
}

func TestOverdueReturnsJSONByDefault(t *testing.T) {
	rec := do(t, newTestHandler(sampleStore()), "/reports/overdue")

	if rec.Code != http.StatusOK {
		t.Fatalf("status = %d, want 200", rec.Code)
	}
	if ct := rec.Header().Get("Content-Type"); !strings.HasPrefix(ct, "application/json") {
		t.Errorf("Content-Type = %q, want application/json", ct)
	}

	var rows []report.OverdueWorkOrder
	if err := json.Unmarshal(rec.Body.Bytes(), &rows); err != nil {
		t.Fatalf("body is not valid JSON: %v", err)
	}
	if len(rows) != 1 || rows[0].Reference != "WO-000003" {
		t.Fatalf("unexpected rows: %+v", rows)
	}
	if rows[0].DaysOpen != 18 {
		t.Errorf("daysOpen = %d, want 18", rows[0].DaysOpen)
	}
}

func TestOverdueReturnsCSVWhenAsked(t *testing.T) {
	rec := do(t, newTestHandler(sampleStore()), "/reports/overdue?format=csv")

	if rec.Code != http.StatusOK {
		t.Fatalf("status = %d, want 200", rec.Code)
	}
	if ct := rec.Header().Get("Content-Type"); !strings.HasPrefix(ct, "text/csv") {
		t.Errorf("Content-Type = %q, want text/csv", ct)
	}
	if cd := rec.Header().Get("Content-Disposition"); !strings.Contains(cd, "overdue-work-orders.csv") {
		t.Errorf("Content-Disposition = %q", cd)
	}

	records, err := csv.NewReader(strings.NewReader(rec.Body.String())).ReadAll()
	if err != nil {
		t.Fatalf("body is not valid CSV: %v", err)
	}
	if len(records) != 2 {
		t.Fatalf("got %d records (including header), want 2", len(records))
	}
}

func TestOverdueWithNoResultsReturnsAnEmptyJSONArray(t *testing.T) {
	rec := do(t, newTestHandler(stubStore{rows: nil}), "/reports/overdue")

	if got := strings.TrimSpace(rec.Body.String()); got != "[]" {
		t.Errorf("body = %q, want []", got)
	}
}

func TestOverdueRejectsANonNumericDays(t *testing.T) {
	rec := do(t, newTestHandler(sampleStore()), "/reports/overdue?days=soon")

	if rec.Code != http.StatusBadRequest {
		t.Fatalf("status = %d, want 400", rec.Code)
	}
}

func TestOverdueRejectsANonPositiveDays(t *testing.T) {
	for _, target := range []string{"/reports/overdue?days=0", "/reports/overdue?days=-3"} {
		rec := do(t, newTestHandler(sampleStore()), target)
		if rec.Code != http.StatusBadRequest {
			t.Errorf("%s: status = %d, want 400", target, rec.Code)
		}
	}
}

func TestOverdueRejectsAnUnknownFormat(t *testing.T) {
	rec := do(t, newTestHandler(sampleStore()), "/reports/overdue?format=pdf")

	if rec.Code != http.StatusBadRequest {
		t.Fatalf("status = %d, want 400", rec.Code)
	}
}

func TestOverdueReturns500WhenTheStoreFails(t *testing.T) {
	rec := do(t, newTestHandler(stubStore{err: errors.New("connection refused")}), "/reports/overdue")

	if rec.Code != http.StatusInternalServerError {
		t.Fatalf("status = %d, want 500", rec.Code)
	}
	// The database error must not leak to the caller.
	if strings.Contains(rec.Body.String(), "connection refused") {
		t.Errorf("response leaked the underlying error: %s", rec.Body.String())
	}
}

func TestHealthReportsUp(t *testing.T) {
	rec := do(t, newTestHandler(sampleStore()), "/health")

	if rec.Code != http.StatusOK {
		t.Fatalf("status = %d, want 200", rec.Code)
	}
	if !strings.Contains(rec.Body.String(), "UP") {
		t.Errorf("body = %q", rec.Body.String())
	}
}
