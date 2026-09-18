package main

import (
	"encoding/json"
	"log/slog"
	"net/http"
	"strconv"

	"github.com/UmutAkbayin/fametrics/screener/db"
	"github.com/UmutAkbayin/fametrics/screener/finance"
	"github.com/jackc/pgx/v5/pgxpool"
)

// candidatesHandler serves GET /candidates?limit=N: companies that passed
// the hard filter, ordered by quality_score descending, capped at limit
// (default finance.TopCompaniesCount). This is what core calls to fetch the
// value-investing shortlist before enriching it with live prices.
func candidatesHandler(pool *pgxpool.Pool) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		limit := finance.TopCompaniesCount
		if raw := r.URL.Query().Get("limit"); raw != "" {
			parsed, err := strconv.Atoi(raw)
			if err != nil || parsed <= 0 {
				http.Error(w, "limit must be a positive integer", http.StatusBadRequest)
				return
			}
			limit = parsed
		}

		candidates, err := db.New(pool).ListCandidates(r.Context(), int32(limit))
		if err != nil {
			slog.Error("failed to list candidates", "error", err)
			http.Error(w, "internal error", http.StatusInternalServerError)
			return
		}

		w.Header().Set("Content-Type", "application/json")
		if err := json.NewEncoder(w).Encode(candidates); err != nil {
			slog.Error("failed to encode candidates response", "error", err)
		}
	}
}
