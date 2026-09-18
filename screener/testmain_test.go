package main

import (
	"context"
	"log"
	"os"
	"testing"

	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/testcontainers/testcontainers-go/modules/postgres"
)

// testPool is shared across every test in this package: starting a Postgres
// container is the expensive part (seconds), so it happens once in TestMain
// rather than once per test.
var testPool *pgxpool.Pool

func TestMain(m *testing.M) {
	code, err := runWithContainer(m)
	if err != nil {
		log.Fatal(err)
	}
	os.Exit(code)
}

// runWithContainer does the setup/teardown around m.Run(). It's a separate
// function from TestMain because os.Exit does not run deferred calls — if
// the container start/pool-close deferrals were in TestMain itself, calling
// os.Exit(m.Run()) there would skip them and leak the container.
func runWithContainer(m *testing.M) (int, error) {
	ctx := context.Background()

	pgContainer, err := postgres.Run(ctx, "postgres:17",
		postgres.WithDatabase("screener_test"),
		postgres.WithUsername("postgres"),
		postgres.WithPassword("postgres"),
		postgres.WithInitScripts("db/schema.sql"), // same file docker-compose.yml uses
		postgres.BasicWaitStrategies(),
	)
	if err != nil {
		return 1, err
	}
	defer func() {
		if err := pgContainer.Terminate(ctx); err != nil {
			log.Printf("failed to terminate postgres container: %v", err)
		}
	}()

	connStr, err := pgContainer.ConnectionString(ctx, "sslmode=disable")
	if err != nil {
		return 1, err
	}

	pool, err := pgxpool.New(ctx, connStr)
	if err != nil {
		return 1, err
	}
	defer pool.Close()

	testPool = pool
	return m.Run(), nil
}

// truncateTables resets both tables between tests so they don't see each
// other's rows; called at the start of a test rather than the end, so a
// failed test leaves its data behind for inspection instead of wiping it.
func truncateTables(t *testing.T) {
	t.Helper()
	if _, err := testPool.Exec(context.Background(), "TRUNCATE companies, ingestion_runs RESTART IDENTITY"); err != nil {
		t.Fatalf("failed to truncate tables: %v", err)
	}
}
