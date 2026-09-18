package finance

import (
	"fmt"
	"log/slog"
	"os"
	"path/filepath"
)

// topCompaniesCount specifies how many companies should be saved to the database
// based on a ranking algorithm
const TopCompaniesCount = 200

// BaseURL is the SEC page listing the financial statement data set zip files.
const baseURL = "https://www.sec.gov/data-research/sec-markets-data/financial-statement-data-sets"

// latestQuarterCount is how many of the newest quarterly zips to keep in sync.
// Companies don't all file their 10-K in the same quarter, so a single
// quarter's data set misses filings that a few quarters together cover.
const latestQuarterCount = 4

// CalculateTopCompanies runs the full SEC ingestion pipeline: download the
// latest quarterly data sets, reduce them to one 10-K per company, extract
// fundamentals, derive metrics, and apply the hard filter and quality score.
// It returns the resulting candidates and the most recent quarter covered,
// for the caller to persist — this package stays free of any database
// dependency so it can be unit-tested without one, as it has been all along.
func CalculateTopCompanies(resourcesDir string) ([]Candidate, string, error) {
	err := updateLatestStatements(baseURL, resourcesDir)
	if err != nil {
		return nil, "", err
	}

	zipPaths, err := filepath.Glob(filepath.Join(resourcesDir, "*.zip"))
	if err != nil {
		return nil, "", err
	}

	var submissions []Submission
	for _, filename := range zipPaths {
		subs, err := readTenKSubmissions(filename)
		if err != nil {
			slog.Error("failed to read submissions", "filename", filename, "error", err)
			continue
		}
		submissions = append(submissions, subs...)
	}

	latest := latestTenKByCIK(submissions)
	slog.Info("collected 10-K submissions", "count", len(submissions), "companies", len(latest))

	fundamentals, err := ExtractFundamentals(resourcesDir, latest)
	if err != nil {
		return nil, "", err
	}
	slog.Info("extracted fundamentals", "companies", len(fundamentals))

	metrics := make(map[string]Metrics, len(fundamentals))
	for adsh, f := range fundamentals {
		metrics[adsh] = ComputeMetrics(f)
	}
	slog.Info("computed derived metrics", "companies", len(metrics))

	candidates := BuildCandidates(latest, fundamentals, metrics)
	ApplyHardFilters(candidates)
	ComputeQualityScores(candidates)

	passing := 0
	var latestQuarter string
	for _, c := range candidates {
		if c.PassesHardFilter {
			passing++
		}
	}
	for _, sub := range latest {
		// SourceZip is "YYYYqQ" (e.g. "2026q2"), fixed-width enough that
		// plain string comparison sorts it chronologically, same trick as
		// the Filed-date comparison in latestTenKByCIK.
		if sub.SourceZip > latestQuarter {
			latestQuarter = sub.SourceZip
		}
	}
	slog.Info("applied hard filters", "companies", len(candidates), "passing", passing)

	return candidates, latestQuarter, nil
}

// updateLatestStatements finds the newest latestQuarterCount SEC financial
// statement zips listed on pageURL and downloads any that are missing from
// resourcesDir.
func updateLatestStatements(pageURL, resourcesDir string) error {
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
