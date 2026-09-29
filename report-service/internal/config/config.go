// Package config reads the service settings from the environment.
package config

import (
	"fmt"
	"os"
	"strconv"
	"time"
)

type Config struct {
	Port            string
	DatabaseURL     string
	DefaultDays     int
	ShutdownTimeout time.Duration
}

// Load reads configuration from the environment, falling back to values that
// work against the docker compose stack.
func Load() (Config, error) {
	cfg := Config{
		Port:        env("PORT", "8081"),
		DatabaseURL: env("DATABASE_URL", "postgres://fieldops:fieldops@localhost:5432/fieldops?sslmode=disable"),
	}

	days, err := strconv.Atoi(env("DEFAULT_OVERDUE_DAYS", "7"))
	if err != nil {
		return Config{}, fmt.Errorf("DEFAULT_OVERDUE_DAYS must be an integer: %w", err)
	}
	if days <= 0 {
		return Config{}, fmt.Errorf("DEFAULT_OVERDUE_DAYS must be greater than zero, got %d", days)
	}
	cfg.DefaultDays = days

	timeout, err := time.ParseDuration(env("SHUTDOWN_TIMEOUT", "10s"))
	if err != nil {
		return Config{}, fmt.Errorf("SHUTDOWN_TIMEOUT must be a duration: %w", err)
	}
	cfg.ShutdownTimeout = timeout

	return cfg, nil
}

func env(key, fallback string) string {
	if value, ok := os.LookupEnv(key); ok && value != "" {
		return value
	}
	return fallback
}
