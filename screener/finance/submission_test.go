package finance

import (
	"archive/zip"
	"os"
	"path/filepath"
	"strings"
	"testing"
)

func TestParseTenKSubmissions_FiltersByForm(t *testing.T) {
	data := strings.Join([]string{
		"adsh\tcik\tname\tsic\tform\tperiod\tfy\tfp\tfiled",
		"0001\t1\tACME, INC.\t1000\t10-K\t20250101\t2025\tFY\t20250201",
		"0002\t2\tOTHERCO\t2000\t10-Q\t20250101\t2025\tQ1\t20250201",
	}, "\n")

	got, err := parseTenKSubmissions(strings.NewReader(data))
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}

	if len(got) != 1 {
		t.Fatalf("expected 1 submission, got %d", len(got))
	}
	want := Submission{ADSH: "0001", CIK: "1", Name: "ACME, INC.", SIC: "1000", Form: "10-K", Period: "20250101", FY: "2025", FP: "FY", Filed: "20250201"}
	if got[0] != want {
		t.Errorf("got %+v, want %+v", got[0], want)
	}
}

func TestParseTenKSubmissions_MissingRequiredColumn(t *testing.T) {
	data := "adsh\tcik\tname\tsic\tperiod\tfy\tfp\tfiled\n0001\t1\tACME\t1000\t20250101\t2025\tFY\t20250201"

	if _, err := parseTenKSubmissions(strings.NewReader(data)); err == nil {
		t.Fatal("expected an error when the form column is missing")
	}
}

func TestParseTenKSubmissions_SkipsMalformedRow(t *testing.T) {
	data := strings.Join([]string{
		"adsh\tcik\tname\tsic\tform\tperiod\tfy\tfp\tfiled",
		"0001\t1\tACME\t1000", // too few fields, must not panic on index out of range
		"0002\t2\tOTHERCO\t2000\t10-K\t20250101\t2025\tFY\t20250201",
	}, "\n")

	got, err := parseTenKSubmissions(strings.NewReader(data))
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if len(got) != 1 || got[0].ADSH != "0002" {
		t.Errorf("expected only the well-formed 10-K row to survive, got %+v", got)
	}
}

func TestParseTenKSubmissions_EmptyInput(t *testing.T) {
	if _, err := parseTenKSubmissions(strings.NewReader("")); err == nil {
		t.Fatal("expected an error for a file with no header row")
	}
}

func TestReadTenKSubmissions_MissingSubTxt(t *testing.T) {
	zipPath := filepath.Join(t.TempDir(), "test.zip")
	writeTestZip(t, zipPath, map[string]string{
		"num.txt": "adsh\tvalue\n0001\t100",
	})

	if _, err := readTenKSubmissions(zipPath); err == nil {
		t.Fatal("expected an error when sub.txt is absent from the zip")
	}
}

func TestReadTenKSubmissions_FindsSubTxtRegardlessOfPath(t *testing.T) {
	zipPath := filepath.Join(t.TempDir(), "test.zip")
	writeTestZip(t, zipPath, map[string]string{
		"some/nested/sub.txt": "adsh\tcik\tname\tsic\tform\tperiod\tfy\tfp\tfiled\n0001\t1\tACME\t1000\t10-K\t20250101\t2025\tFY\t20250201",
	})

	got, err := readTenKSubmissions(zipPath)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if len(got) != 1 || got[0].ADSH != "0001" {
		t.Errorf("expected to find sub.txt by base name, got %+v", got)
	}
}

func TestLatestTenKByCIK_KeepsMostRecentFiledPerCIK(t *testing.T) {
	submissions := []Submission{
		{ADSH: "0001", CIK: "1", Filed: "20240301"},
		{ADSH: "0002", CIK: "1", Filed: "20250301"}, // later filing for the same company
		{ADSH: "0003", CIK: "1", Filed: "20230301"},
	}

	got := latestTenKByCIK(submissions)

	if len(got) != 1 {
		t.Fatalf("expected 1 company, got %d", len(got))
	}
	if got["1"].ADSH != "0002" {
		t.Errorf("expected the most recently filed submission (0002), got %+v", got["1"])
	}
}

func TestLatestTenKByCIK_KeepsOneEntryPerDistinctCIK(t *testing.T) {
	submissions := []Submission{
		{ADSH: "0001", CIK: "1", Filed: "20250301"},
		{ADSH: "0002", CIK: "2", Filed: "20250301"},
	}

	got := latestTenKByCIK(submissions)

	if len(got) != 2 {
		t.Fatalf("expected 2 companies, got %d", len(got))
	}
	if got["1"].ADSH != "0001" || got["2"].ADSH != "0002" {
		t.Errorf("expected each CIK to keep its own submission, got %+v", got)
	}
}

func TestLatestTenKByCIK_EmptyInput(t *testing.T) {
	got := latestTenKByCIK(nil)

	if len(got) != 0 {
		t.Errorf("expected an empty map, got %+v", got)
	}
}

// writeTestZip creates a zip file at zipPath containing the given files.
func writeTestZip(t *testing.T, zipPath string, files map[string]string) {
	t.Helper()

	f, err := os.Create(zipPath)
	if err != nil {
		t.Fatalf("failed to create zip file: %v", err)
	}
	defer f.Close()

	w := zip.NewWriter(f)
	for name, content := range files {
		entry, err := w.Create(name)
		if err != nil {
			t.Fatalf("failed to add %s to zip: %v", name, err)
		}
		if _, err := entry.Write([]byte(content)); err != nil {
			t.Fatalf("failed to write %s content: %v", name, err)
		}
	}
	if err := w.Close(); err != nil {
		t.Fatalf("failed to finalize zip: %v", err)
	}
}
