package finance

import (
	"archive/zip"
	"os"
	"path/filepath"
	"strings"
	"testing"
	"time"
)

// mustParseDate parses a sub.txt-style YYYYMMDD date for use in test fixtures.
func mustParseDate(t *testing.T, s string) time.Time {
	t.Helper()
	date, err := time.Parse(dateLayout, s)
	if err != nil {
		t.Fatalf("failed to parse test date %q: %v", s, err)
	}
	return date
}

// submissionsEqual compares two Submissions field by field. time.Time fields
// must be compared with Equal rather than ==, since struct equality also
// compares the Location pointer and any monotonic reading, which need not
// match even for the same instant.
func submissionsEqual(a, b Submission) bool {
	return a.ADSH == b.ADSH &&
		a.CIK == b.CIK &&
		a.Name == b.Name &&
		a.SIC == b.SIC &&
		a.Form == b.Form &&
		a.Period.Equal(b.Period) &&
		a.FY == b.FY &&
		a.FP == b.FP &&
		a.Filed.Equal(b.Filed) &&
		a.SourceZip == b.SourceZip
}

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
	want := Submission{
		ADSH: "0001", CIK: "1", Name: "ACME, INC.", SIC: "1000", Form: "10-K",
		Period: mustParseDate(t, "20250101"), FY: "2025", FP: "FY",
		Filed: mustParseDate(t, "20250201"),
	}
	if !submissionsEqual(got[0], want) {
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

func TestParseTenKSubmissions_SkipsRowWithUnparseableDate(t *testing.T) {
	data := strings.Join([]string{
		"adsh\tcik\tname\tsic\tform\tperiod\tfy\tfp\tfiled",
		"0001\t1\tACME\t1000\t10-K\tnot-a-date\t2025\tFY\t20250201",
		"0002\t2\tOTHERCO\t2000\t10-K\t20250101\t2025\tFY\t20250201",
	}, "\n")

	got, err := parseTenKSubmissions(strings.NewReader(data))
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if len(got) != 1 || got[0].ADSH != "0002" {
		t.Errorf("expected the row with the unparseable period date to be skipped, got %+v", got)
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

func TestReadTenKSubmissions_TagsSourceZip(t *testing.T) {
	zipPath := filepath.Join(t.TempDir(), "2026q2.zip")
	writeTestZip(t, zipPath, map[string]string{
		"sub.txt": "adsh\tcik\tname\tsic\tform\tperiod\tfy\tfp\tfiled\n0001\t1\tACME\t1000\t10-K\t20250101\t2025\tFY\t20250201",
	})

	got, err := readTenKSubmissions(zipPath)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if len(got) != 1 || got[0].SourceZip != "2026q2" {
		t.Errorf("expected rows to be tagged with SourceZip %q, got %+v", "2026q2", got)
	}
}

func TestLatestTenKByCIK_PrefersLaterPeriodOverFiled(t *testing.T) {
	submissions := []Submission{
		// Newer fiscal year, filed first.
		{ADSH: "0001", CIK: "1", Period: mustParseDate(t, "20241231"), Filed: mustParseDate(t, "20250301")},
		// Restated older fiscal year, filed later — must not win despite the later Filed date.
		{ADSH: "0002", CIK: "1", Period: mustParseDate(t, "20231231"), Filed: mustParseDate(t, "20250601")},
	}

	got := latestTenKByCIK(submissions)

	if got["1"].ADSH != "0001" {
		t.Errorf("expected the later-period submission (0001) to win, got %+v", got["1"])
	}
}

func TestLatestTenKByCIK_FiledBreaksPeriodTie(t *testing.T) {
	submissions := []Submission{
		{ADSH: "0001", CIK: "1", Period: mustParseDate(t, "20241231"), Filed: mustParseDate(t, "20250301")},
		// Refiling of the same fiscal period under a new ADSH.
		{ADSH: "0002", CIK: "1", Period: mustParseDate(t, "20241231"), Filed: mustParseDate(t, "20250401")},
	}

	got := latestTenKByCIK(submissions)

	if got["1"].ADSH != "0002" {
		t.Errorf("expected the later-filed refiling (0002) to win, got %+v", got["1"])
	}
}

func TestLatestTenKByCIK_KeepsOneEntryPerDistinctCIK(t *testing.T) {
	submissions := []Submission{
		{ADSH: "0001", CIK: "1", Period: mustParseDate(t, "20241231"), Filed: mustParseDate(t, "20250301")},
		{ADSH: "0002", CIK: "2", Period: mustParseDate(t, "20241231"), Filed: mustParseDate(t, "20250301")},
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
