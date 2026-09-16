package finance

import (
	"path/filepath"
	"strings"
	"testing"
)

const numFactHeader = "adsh\ttag\tversion\tddate\tqtrs\tuom\tsegments\tcoreg\tvalue\tfootnote"

func TestBuildFundamentals_CurrentAndPriorYear(t *testing.T) {
	period := mustParseDate(t, "20241231")
	tagFacts := map[string][]dateValue{
		"NetIncomeLoss": {
			{ddate: mustParseDate(t, "20231231"), value: 900},
			{ddate: period, value: 1000},
		},
	}

	got := buildFundamentals(tagFacts, period)

	if got.NetIncomeLoss == nil || *got.NetIncomeLoss != 1000 {
		t.Errorf("expected current NetIncomeLoss 1000, got %v", got.NetIncomeLoss)
	}
	if got.NetIncomeLossPrior == nil || *got.NetIncomeLossPrior != 900 {
		t.Errorf("expected prior NetIncomeLoss 900, got %v", got.NetIncomeLossPrior)
	}
}

func TestBuildFundamentals_MissingTagLeavesFieldNil(t *testing.T) {
	got := buildFundamentals(map[string][]dateValue{}, mustParseDate(t, "20241231"))

	if got.Assets != nil {
		t.Errorf("expected nil Assets when the tag is absent, got %v", *got.Assets)
	}
}

func TestBuildFundamentals_PrefersFirstCandidateTag(t *testing.T) {
	period := mustParseDate(t, "20241231")
	tagFacts := map[string][]dateValue{
		"Revenues": {{ddate: period, value: 500}},
		"RevenueFromContractWithCustomerExcludingAssessedTax": {{ddate: period, value: 999}},
	}

	got := buildFundamentals(tagFacts, period)

	if got.Revenue == nil || *got.Revenue != 500 {
		t.Errorf("expected Revenues (higher priority) to win with 500, got %v", got.Revenue)
	}
}

func TestBuildFundamentals_FallsBackWhenPreferredTagMissingCurrentYear(t *testing.T) {
	period := mustParseDate(t, "20241231")
	tagFacts := map[string][]dateValue{
		// Revenues exists for this company, but not for the current fiscal year.
		"Revenues": {{ddate: mustParseDate(t, "20231231"), value: 500}},
		"RevenueFromContractWithCustomerExcludingAssessedTax": {{ddate: period, value: 999}},
	}

	got := buildFundamentals(tagFacts, period)

	if got.Revenue == nil || *got.Revenue != 999 {
		t.Errorf("expected fallback tag to win with 999, got %v", got.Revenue)
	}
}

func TestReadNumFacts_FiltersCoregSegmentsQtrsAndUom(t *testing.T) {
	rows := []string{
		numFactHeader,
		"0001-1\tAssets\tus-gaap/2025\t20241231\t0\tUSD\t\t\t1000.0000\t",           // clean, wanted
		"0001-1\tAssets\tus-gaap/2025\t20241231\t0\tUSD\t\tParentCo\t2000.0000\t",   // has coreg, excluded
		"0001-1\tAssets\tus-gaap/2025\t20241231\t0\tUSD\tSegment=X;\t\t3000.0000\t", // has segments, excluded
		"0001-1\tAssets\tus-gaap/2025\t20241231\t1\tUSD\t\t\t4000.0000\t",           // wrong qtrs, excluded
		"0001-1\tAssets\tus-gaap/2025\t20241231\t0\tEUR\t\t\t5000.0000\t",           // wrong uom, excluded
		"0002-1\tAssets\tus-gaap/2025\t20241231\t0\tUSD\t\t\t9999.0000\t",           // not a target ADSH, excluded
		"0001-1\tUnwantedTag\tus-gaap/2025\t20241231\t0\tUSD\t\t\t1.0000\t",         // not a wanted tag, excluded
	}
	zipPath := writeNumTestZip(t, rows)

	facts, err := readNumFacts(zipPath, map[string]struct{}{"0001-1": {}})
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}

	got := facts["0001-1"]["Assets"]
	if len(got) != 1 || got[0].value != 1000 {
		t.Errorf("expected exactly one clean Assets value of 1000, got %+v", got)
	}
}

func TestExtractFundamentals_GroupsByZipAndJoinsOnADSH(t *testing.T) {
	rows := []string{
		numFactHeader,
		"0001-1\tAssets\tus-gaap/2025\t20241231\t0\tUSD\t\t\t1000.0000\t",
	}
	zipPath := writeNumTestZip(t, rows)
	resourcesDir := filepath.Dir(zipPath)
	zipName := strings.TrimSuffix(filepath.Base(zipPath), filepath.Ext(zipPath))

	submissions := map[string]Submission{
		"1": {CIK: "1", ADSH: "0001-1", Period: mustParseDate(t, "20241231"), SourceZip: zipName},
	}

	got, err := ExtractFundamentals(resourcesDir, submissions)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}

	f, ok := got["0001-1"]
	if !ok || f.Assets == nil || *f.Assets != 1000 {
		t.Errorf("expected ADSH 0001-1 to have Assets 1000, got %+v (present: %v)", f, ok)
	}
}

// writeNumTestZip writes rows as num.txt inside a new zip and returns its path.
func writeNumTestZip(t *testing.T, rows []string) string {
	t.Helper()
	zipPath := filepath.Join(t.TempDir(), "test.zip")
	writeTestZip(t, zipPath, map[string]string{
		"num.txt": strings.Join(rows, "\n"),
	})
	return zipPath
}
