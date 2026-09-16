package main

import (
	"encoding/json"
	"log/slog"
	"net/http"
	"os"

	"github.com/UmutAkbayin/fametrics/screener/finance"
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

	err := finance.CalculateTopCompanies("resources", finance.TopCompaniesCount)
	if err != nil {
		panic(err)
	}

	slog.Info("screener listening on :8081")
	if err := http.ListenAndServe(":8081", nil); err != nil {
		slog.Error("server failed", "error", err)
		os.Exit(1)
	}
}
