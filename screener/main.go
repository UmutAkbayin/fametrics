package main

import (
	"encoding/json"
	"fmt"
	"log/slog"
	"net/http"
	"os"
	"path"

	"github.com/UmutAkbayin/fametrics/screener/finance"
	"github.com/joho/godotenv"
)

const baseURL = "https://www.sec.gov/data-research/sec-markets-data/financial-statement-data-sets"

func main() {
	if err := godotenv.Load(); err != nil {
		slog.Info("no .env file found, relying on environment variables")
	}

	http.HandleFunc("/health", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(map[string]string{"status": "ok"})
	})

	fmt.Println("Search the SEC website for the latest file...")
	latestURL, err := finance.FindLatestZipURL(baseURL)
	if err != nil {
		panic(err)
	}

	fmt.Printf("Latest file found: %s\n", latestURL)
	outFile := "resources/" + path.Base(latestURL)

	if _, err := os.Stat(outFile); err == nil {
		fmt.Printf("File is already up to date: %s\n", outFile)
	} else {
		fmt.Println("Downloading the file...")
		if err := finance.DownloadFile(latestURL, outFile); err != nil {
			panic(err)
		}
		fmt.Printf("Successfully saved as: %s\n", outFile)
	}

	slog.Info("screener listening on :8081")
	if err := http.ListenAndServe(":8081", nil); err != nil {
		slog.Error("server failed", "error", err)
		os.Exit(1)
	}
}
