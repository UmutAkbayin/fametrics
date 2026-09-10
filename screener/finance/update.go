package finance

import (
	"log/slog"
	"os"
	"path"
)

const baseURL = "https://www.sec.gov/data-research/sec-markets-data/financial-statement-data-sets"

// UpdateLatestStatements finds the newest SEC financial statement zip and
// downloads it into resourcesDir if it isn't already present there. It
// returns the path to the file.
func UpdateLatestStatements(resourcesDir string) (string, error) {
	slog.Info("scanning SEC website for latest file")
	latestURL, err := FindLatestZipURL(baseURL)
	if err != nil {
		return "", err
	}

	outFile := path.Join(resourcesDir, path.Base(latestURL))

	if _, err := os.Stat(outFile); err == nil {
		slog.Info("file already up to date", "file", outFile)
		return outFile, nil
	}

	slog.Info("downloading file", "url", latestURL)
	if err := DownloadFile(latestURL, outFile); err != nil {
		return "", err
	}
	slog.Info("file saved", "file", outFile)

	return outFile, nil
}
