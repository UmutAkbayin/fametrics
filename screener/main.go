package main

import (
	"context"
	"encoding/json"
	"errors"
	"log/slog"
	"net/http"
	"os"

	"github.com/UmutAkbayin/fametrics/screener/db"
	"github.com/UmutAkbayin/fametrics/screener/finance"
	"github.com/jackc/pgx/v5"
	"github.com/joho/godotenv"
)

func main() {
	if err := godotenv.Load(); err != nil {
		slog.Info("no .env file found, relying on environment variables")
	}

	http.HandleFunc("/health", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(map[string]string{"status": "ok"})
	})

	ctx := context.Background()

	pool, err := db.Connect(ctx)
	if err != nil {
		panic(err)
	}
	defer pool.Close()

	http.HandleFunc("GET /candidates", candidatesHandler(pool))

	zipPaths, err := finance.SyncLatestZips("resources")
	if err != nil {
		panic(err)
	}
	latestQuarter := finance.LatestQuarterLabel(zipPaths)

	lastIngested, err := db.New(pool).GetLatestIngestionQuarter(ctx)
	if err != nil && !errors.Is(err, pgx.ErrNoRows) {
		panic(err)
	}

	if lastIngested == latestQuarter {
		slog.Info("no new SEC data since the last ingestion run, skipping recompute", "quarter", latestQuarter)
	} else {
		candidates, quarter, err := finance.CalculateTopCompanies("resources", zipPaths)
		if err != nil {
			panic(err)
		}

		if err := persistCandidates(ctx, pool, candidates, quarter); err != nil {
			panic(err)
		}
		slog.Info("persisted candidates", "count", len(candidates), "quarter", quarter)
	}

	slog.Info("screener listening on :8081")
	if err := http.ListenAndServe(":8081", nil); err != nil {
		slog.Error("server failed", "error", err)
		os.Exit(1)
	}
}
