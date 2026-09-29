package config

import (
	"testing"
	"time"
)

func TestLoadFallsBackToDefaults(t *testing.T) {
	cfg, err := Load()
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if cfg.Port != "8081" {
		t.Errorf("Port = %q, want 8081", cfg.Port)
	}
	if cfg.DefaultDays != 7 {
		t.Errorf("DefaultDays = %d, want 7", cfg.DefaultDays)
	}
	if cfg.ShutdownTimeout != 10*time.Second {
		t.Errorf("ShutdownTimeout = %v, want 10s", cfg.ShutdownTimeout)
	}
	if cfg.DatabaseURL == "" {
		t.Error("DatabaseURL is empty")
	}
}

func TestLoadReadsTheEnvironment(t *testing.T) {
	t.Setenv("PORT", "9090")
	t.Setenv("DEFAULT_OVERDUE_DAYS", "14")
	t.Setenv("SHUTDOWN_TIMEOUT", "30s")
	t.Setenv("DATABASE_URL", "postgres://u:p@db:5432/x?sslmode=disable")

	cfg, err := Load()
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if cfg.Port != "9090" {
		t.Errorf("Port = %q, want 9090", cfg.Port)
	}
	if cfg.DefaultDays != 14 {
		t.Errorf("DefaultDays = %d, want 14", cfg.DefaultDays)
	}
	if cfg.ShutdownTimeout != 30*time.Second {
		t.Errorf("ShutdownTimeout = %v, want 30s", cfg.ShutdownTimeout)
	}
}

func TestLoadTreatsAnEmptyVariableAsUnset(t *testing.T) {
	t.Setenv("PORT", "")

	cfg, err := Load()
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if cfg.Port != "8081" {
		t.Errorf("Port = %q, want the 8081 fallback", cfg.Port)
	}
}

func TestLoadRejectsABadDayWindow(t *testing.T) {
	for _, value := range []string{"soon", "0", "-5"} {
		t.Run(value, func(t *testing.T) {
			t.Setenv("DEFAULT_OVERDUE_DAYS", value)
			if _, err := Load(); err == nil {
				t.Errorf("DEFAULT_OVERDUE_DAYS=%q was accepted, want an error", value)
			}
		})
	}
}

func TestLoadRejectsABadShutdownTimeout(t *testing.T) {
	t.Setenv("SHUTDOWN_TIMEOUT", "ten seconds")

	if _, err := Load(); err == nil {
		t.Error("an invalid SHUTDOWN_TIMEOUT was accepted, want an error")
	}
}
