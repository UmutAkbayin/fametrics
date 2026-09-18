package finance

import (
	"archive/zip"
	"bufio"
	"fmt"
	"io"
	"log/slog"
	"path/filepath"
	"strings"
	"time"
)

// dateLayout is the "period"/"filed" column format in sub.txt: an 8-digit
// YYYYMMDD date, expressed via Go's reference-time layout.
const dateLayout = "20060102"

// Submission is one row of a quarter's sub.txt, reduced to the columns needed
// to identify and later rank a company's 10-K filing.
type Submission struct {
	ADSH   string // unique submission id, used to join with num.txt/pre.txt
	CIK    string
	Name   string
	SIC    string
	Form   string
	Period time.Time // fiscal period end date ("period" column)
	FY     string
	FP     string
	Filed  time.Time // SEC acceptance date ("filed" column)

	// SourceZip is the quarterly data set this row came from (e.g. "2026q2"),
	// derived from the zip's filename rather than read from sub.txt itself.
	// A given ADSH's sub.txt row and its num.txt facts always live in the
	// same zip, so this is how step 3 will know where to look them up.
	SourceZip string
}

// tenKSubmissionColumns are the sub.txt header names read into a Submission.
var tenKSubmissionColumns = []string{"adsh", "cik", "name", "sic", "form", "period", "fy", "fp", "filed"}

// readTenKSubmissions opens sub.txt inside the SEC financial statement zip at
// zipPath and returns the rows whose form is "10-K".
func readTenKSubmissions(zipPath string) ([]Submission, error) {
	reader, err := zip.OpenReader(zipPath)
	if err != nil {
		return nil, err
	}
	defer reader.Close()

	var subFile *zip.File
	for _, f := range reader.File {
		if filepath.Base(f.Name) == "sub.txt" {
			subFile = f
			break
		}
	}
	if subFile == nil {
		return nil, fmt.Errorf("sub.txt not found in %s", zipPath)
	}

	rc, err := subFile.Open()
	if err != nil {
		return nil, err
	}
	defer rc.Close()

	submissions, err := parseTenKSubmissions(rc)
	if err != nil {
		return nil, err
	}

	sourceZip := quarterLabel(zipPath)
	for i := range submissions {
		submissions[i].SourceZip = sourceZip
	}

	return submissions, nil
}

// quarterLabel derives a quarterly data set's "YYYYqQ" label (e.g. "2026q2")
// from its zip filename, rather than any content inside the zip.
func quarterLabel(zipPath string) string {
	return strings.TrimSuffix(filepath.Base(zipPath), filepath.Ext(zipPath))
}

// parseTenKSubmissions reads tab-delimited submission rows from r, keeping
// only those whose form is "10-K".
func parseTenKSubmissions(r io.Reader) ([]Submission, error) {
	scanner := bufio.NewScanner(r)
	scanner.Buffer(make([]byte, 0, 64*1024), 1024*1024)

	if !scanner.Scan() {
		if err := scanner.Err(); err != nil {
			return nil, err
		}
		return nil, fmt.Errorf("sub.txt has no header row")
	}

	header := strings.Split(scanner.Text(), "\t")
	columnIndex := make(map[string]int, len(header))
	for i, name := range header {
		columnIndex[name] = i
	}
	for _, name := range tenKSubmissionColumns {
		if _, ok := columnIndex[name]; !ok {
			return nil, fmt.Errorf("sub.txt is missing expected column %q", name)
		}
	}

	var submissions []Submission
	for scanner.Scan() {
		fields := strings.Split(scanner.Text(), "\t")
		if len(fields) != len(header) {
			continue
		}
		if fields[columnIndex["form"]] != "10-K" {
			continue
		}

		adsh := fields[columnIndex["adsh"]]
		period, err := time.Parse(dateLayout, fields[columnIndex["period"]])
		if err != nil {
			slog.Warn("sub.txt row has unparseable period date, skipping", "adsh", adsh, "error", err)
			continue
		}
		filed, err := time.Parse(dateLayout, fields[columnIndex["filed"]])
		if err != nil {
			slog.Warn("sub.txt row has unparseable filed date, skipping", "adsh", adsh, "error", err)
			continue
		}

		submissions = append(submissions, Submission{
			ADSH:   adsh,
			CIK:    fields[columnIndex["cik"]],
			Name:   fields[columnIndex["name"]],
			SIC:    fields[columnIndex["sic"]],
			Form:   fields[columnIndex["form"]],
			Period: period,
			FY:     fields[columnIndex["fy"]],
			FP:     fields[columnIndex["fp"]],
			Filed:  filed,
		})
	}
	if err := scanner.Err(); err != nil {
		return nil, err
	}

	return submissions, nil
}

// latestTenKByCIK reduces submissions to at most one 10-K per company (CIK).
// The later fiscal Period wins. If two rows share the same Period (a
// refiling under a new ADSH), the one accepted later (Filed) wins instead.
// Filed is deliberately not the primary key: a restated report for an older
// fiscal year can be filed after a newer annual report, and comparing Filed
// alone would incorrectly prefer the older restated data.
func latestTenKByCIK(submissions []Submission) map[string]Submission {
	latest := make(map[string]Submission, len(submissions))
	for _, sub := range submissions {
		existing, ok := latest[sub.CIK]
		if !ok ||
			sub.Period.After(existing.Period) ||
			(sub.Period.Equal(existing.Period) && sub.Filed.After(existing.Filed)) {
			latest[sub.CIK] = sub
		}
	}
	return latest
}
