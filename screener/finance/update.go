package finance

import (
	"fmt"
	"log/slog"
	"os"
	"path/filepath"
)

// BaseURL is the SEC page listing the financial statement data set zip files.
const BaseURL = "https://www.sec.gov/data-research/sec-markets-data/financial-statement-data-sets"

// latestQuarterCount is how many of the newest quarterly zips to keep in sync.
// Companies don't all file their 10-K in the same quarter, so a single
// quarter's data set misses filings that a few quarters together cover.
const latestQuarterCount = 4

// UpdateLatestStatements finds the newest latestQuarterCount SEC financial
// statement zips listed on pageURL and downloads any that are missing from
// resourcesDir.
func UpdateLatestStatements(pageURL, resourcesDir string) error {
	slog.Info("scanning SEC website for latest files", "count", latestQuarterCount)
	zipURLs, err := FindLatestZipURLs(pageURL, latestQuarterCount)
	if err != nil {
		return err
	}

	if err := os.MkdirAll(resourcesDir, 0o755); err != nil {
		return fmt.Errorf("failed to create resources directory: %w", err)
	}

	for _, url := range zipURLs {
		outFile := filepath.Join(resourcesDir, filepath.Base(url))

		if _, err := os.Stat(outFile); err == nil {
			slog.Info("file already up to date", "file", outFile)
			continue
		}

		slog.Info("downloading file", "url", url)
		if err := DownloadFile(url, outFile); err != nil {
			slog.Error("failed to download file", "url", url, "error", err)
			continue
		}

		slog.Info("file saved", "file", outFile)
	}
	return nil
}
