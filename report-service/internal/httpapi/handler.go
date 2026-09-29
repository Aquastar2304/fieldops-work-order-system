// Package httpapi exposes the report over HTTP.
package httpapi

import (
	"encoding/json"
	"errors"
	"log/slog"
	"net/http"
	"strconv"

	"github.com/Aquastar2304/fieldops-work-order-system/report-service/internal/report"
)

// Handler serves the report endpoints.
type Handler struct {
	service     *report.Service
	defaultDays int
	logger      *slog.Logger
}

func NewHandler(service *report.Service, defaultDays int, logger *slog.Logger) *Handler {
	if logger == nil {
		logger = slog.Default()
	}
	return &Handler{service: service, defaultDays: defaultDays, logger: logger}
}

// Routes returns the mux with every route this service serves.
func (h *Handler) Routes() *http.ServeMux {
	mux := http.NewServeMux()
	mux.HandleFunc("GET /reports/overdue", h.overdue)
	mux.HandleFunc("GET /health", h.health)
	return mux
}

func (h *Handler) health(w http.ResponseWriter, _ *http.Request) {
	writeJSON(w, http.StatusOK, map[string]string{"status": "UP"})
}

// overdue serves GET /reports/overdue?days=N&format=json|csv.
func (h *Handler) overdue(w http.ResponseWriter, r *http.Request) {
	days := h.defaultDays
	if raw := r.URL.Query().Get("days"); raw != "" {
		parsed, err := strconv.Atoi(raw)
		if err != nil {
			writeError(w, http.StatusBadRequest, "days must be an integer")
			return
		}
		days = parsed
	}

	rows, err := h.service.Overdue(r.Context(), days)
	if err != nil {
		if errors.Is(err, report.ErrInvalidWindow) {
			writeError(w, http.StatusBadRequest, "days must be greater than zero")
			return
		}
		h.logger.Error("overdue report failed", "error", err)
		writeError(w, http.StatusInternalServerError, "could not build the report")
		return
	}

	switch r.URL.Query().Get("format") {
	case "csv":
		w.Header().Set("Content-Type", "text/csv; charset=utf-8")
		w.Header().Set("Content-Disposition", `attachment; filename="overdue-work-orders.csv"`)
		if err := report.WriteCSV(w, rows); err != nil {
			// The status line is already sent, so all that is left is to record it.
			h.logger.Error("writing CSV response failed", "error", err)
		}
	case "", "json":
		writeJSON(w, http.StatusOK, rows)
	default:
		writeError(w, http.StatusBadRequest, "format must be json or csv")
	}
}

func writeJSON(w http.ResponseWriter, status int, body any) {
	w.Header().Set("Content-Type", "application/json; charset=utf-8")
	w.WriteHeader(status)
	if err := json.NewEncoder(w).Encode(body); err != nil {
		slog.Error("writing JSON response failed", "error", err)
	}
}

func writeError(w http.ResponseWriter, status int, message string) {
	writeJSON(w, status, map[string]any{"status": status, "message": message})
}
