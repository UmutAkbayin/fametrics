package finance

import (
	"archive/zip"
	"bufio"
	"fmt"
	"io"
	"path/filepath"
	"strings"
)

// Submission is one row of a quarter's sub.txt, reduced to the columns needed
// to identify and later rank a company's 10-K filing.
type Submission struct {
	ADSH   string // unique submission id, used to join with num.txt/pre.txt
	CIK    string
	Name   string
	SIC    string
	Form   string
	Period string
	FY     string
	FP     string
	Filed  string
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

	return parseTenKSubmissions(rc)
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
		submissions = append(submissions, Submission{
			ADSH:   fields[columnIndex["adsh"]],
			CIK:    fields[columnIndex["cik"]],
			Name:   fields[columnIndex["name"]],
			SIC:    fields[columnIndex["sic"]],
			Form:   fields[columnIndex["form"]],
			Period: fields[columnIndex["period"]],
			FY:     fields[columnIndex["fy"]],
			FP:     fields[columnIndex["fp"]],
			Filed:  fields[columnIndex["filed"]],
		})
	}
	if err := scanner.Err(); err != nil {
		return nil, err
	}

	return submissions, nil
}

// latestTenKByCIK reduces submissions to at most one 10-K per company (CIK),
// keeping the filing with the latest Filed date. Filed is a fixed-width
// zero-padded YYYYMMDD string, so lexicographic comparison matches
// chronological order.
func latestTenKByCIK(submissions []Submission) map[string]Submission {
	latest := make(map[string]Submission, len(submissions))
	for _, sub := range submissions {
		if existing, ok := latest[sub.CIK]; !ok || sub.Filed > existing.Filed {
			latest[sub.CIK] = sub
		}
	}
	return latest
}
