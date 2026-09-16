package finance

import (
	"archive/zip"
	"bufio"
	"fmt"
	"path/filepath"
	"strconv"
	"strings"
	"time"
)

// Fundamentals holds the raw XBRL facts extracted from num.txt for one 10-K
// submission. Fields are pointers because a tag can legitimately be absent
// (no clean, unsegmented row exists for that company) — nil means "unknown",
// distinct from a reported zero, and maps directly to a nullable DB column.
type Fundamentals struct {
	Assets                 *float64
	Liabilities            *float64
	StockholdersEquity     *float64
	NetIncomeLoss          *float64
	NetIncomeLossPrior     *float64
	OperatingIncomeLoss    *float64
	OperatingCashFlow      *float64
	OperatingCashFlowPrior *float64
	Capex                  *float64
	CapexPrior             *float64
	Revenue                *float64
	Cash                   *float64
	LongTermDebt           *float64
	SharesOutstanding      *float64
}

// numFactColumns are the num.txt header names read while scanning.
var numFactColumns = []string{"adsh", "tag", "ddate", "qtrs", "uom", "segments", "coreg", "value"}

// tagRule describes how one XBRL tag must look in num.txt to count as a
// clean, reportable value for it: qtrs distinguishes a point-in-time
// balance-sheet fact ("0") from a full fiscal year duration fact ("4"), kept
// as the same string num.txt uses so no int parsing is needed to compare.
type tagRule struct {
	tag  string
	qtrs string
	uom  string
}

// wantedTags are every tag that feeds a Fundamentals field, including the
// fallback tags used when a company doesn't report the preferred one.
var wantedTags = []tagRule{
	{tag: "Assets", qtrs: "0", uom: "USD"},
	{tag: "Liabilities", qtrs: "0", uom: "USD"},
	{tag: "StockholdersEquity", qtrs: "0", uom: "USD"},
	{tag: "NetIncomeLoss", qtrs: "4", uom: "USD"},
	{tag: "OperatingIncomeLoss", qtrs: "4", uom: "USD"},
	{tag: "NetCashProvidedByUsedInOperatingActivities", qtrs: "4", uom: "USD"},
	{tag: "PaymentsForCapitalExpenditures", qtrs: "4", uom: "USD"},
	{tag: "PaymentsToAcquirePropertyPlantAndEquipment", qtrs: "4", uom: "USD"},
	{tag: "Revenues", qtrs: "4", uom: "USD"},
	{tag: "RevenueFromContractWithCustomerExcludingAssessedTax", qtrs: "4", uom: "USD"},
	{tag: "CashAndCashEquivalentsAtCarryingValue", qtrs: "0", uom: "USD"},
	{tag: "LongTermDebtNoncurrent", qtrs: "0", uom: "USD"},
	{tag: "CommonStockSharesOutstanding", qtrs: "0", uom: "shares"},
	{tag: "EntityCommonStockSharesOutstanding", qtrs: "0", uom: "shares"},
}

// tagIndex looks up a wantedTags entry by tag name while scanning num.txt.
var tagIndex = buildTagIndex(wantedTags)

func buildTagIndex(tags []tagRule) map[string]tagRule {
	index := make(map[string]tagRule, len(tags))
	for _, t := range tags {
		index[t.tag] = t
	}
	return index
}

// dateValue is one clean (coreg=="", segments=="") num.txt value for a given
// tag, at a given balance/period date.
type dateValue struct {
	ddate time.Time
	value float64
}

// ExtractFundamentals reads num.txt from each zip referenced by submissions
// (keyed by CIK, as returned by latestTenKByCIK) and extracts the
// Fundamentals fields for each submission's ADSH. num.txt is opened once per
// distinct SourceZip, not once per company.
func ExtractFundamentals(resourcesDir string, submissions map[string]Submission) (map[string]Fundamentals, error) {
	byZip := make(map[string][]Submission)
	for _, sub := range submissions {
		byZip[sub.SourceZip] = append(byZip[sub.SourceZip], sub)
	}

	results := make(map[string]Fundamentals, len(submissions))
	for zipName, subs := range byZip {
		targetADSH := make(map[string]struct{}, len(subs))
		for _, sub := range subs {
			targetADSH[sub.ADSH] = struct{}{}
		}

		zipPath := filepath.Join(resourcesDir, zipName+".zip")
		facts, err := readNumFacts(zipPath, targetADSH)
		if err != nil {
			return nil, fmt.Errorf("failed to read num.txt from %s: %w", zipPath, err)
		}

		for _, sub := range subs {
			results[sub.ADSH] = buildFundamentals(facts[sub.ADSH], sub.Period)
		}
	}
	return results, nil
}

// readNumFacts streams num.txt inside the zip at zipPath and returns, for
// each ADSH in targetADSH, the clean (coreg=="", segments=="") dateValues for
// every wanted tag it reported at the expected qtrs/uom.
func readNumFacts(zipPath string, targetADSH map[string]struct{}) (map[string]map[string][]dateValue, error) {
	reader, err := zip.OpenReader(zipPath)
	if err != nil {
		return nil, err
	}
	defer reader.Close()

	var numFile *zip.File
	for _, f := range reader.File {
		if filepath.Base(f.Name) == "num.txt" {
			numFile = f
			break
		}
	}
	if numFile == nil {
		return nil, fmt.Errorf("num.txt not found in %s", zipPath)
	}

	rc, err := numFile.Open()
	if err != nil {
		return nil, err
	}
	defer rc.Close()

	scanner := bufio.NewScanner(rc)
	scanner.Buffer(make([]byte, 0, 64*1024), 1024*1024)

	if !scanner.Scan() {
		if err := scanner.Err(); err != nil {
			return nil, err
		}
		return nil, fmt.Errorf("num.txt has no header row")
	}

	header := strings.Split(scanner.Text(), "\t")
	columnIndex := make(map[string]int, len(header))
	for i, name := range header {
		columnIndex[name] = i
	}
	for _, name := range numFactColumns {
		if _, ok := columnIndex[name]; !ok {
			return nil, fmt.Errorf("num.txt is missing expected column %q", name)
		}
	}

	facts := make(map[string]map[string][]dateValue, len(targetADSH))
	for scanner.Scan() {
		fields := strings.Split(scanner.Text(), "\t")
		if len(fields) != len(header) {
			continue // malformed row: field count doesn't match the header, skip rather than index out of range
		}

		adsh := fields[columnIndex["adsh"]]
		if _, wanted := targetADSH[adsh]; !wanted {
			continue
		}
		rule, wanted := tagIndex[fields[columnIndex["tag"]]]
		if !wanted {
			continue
		}
		if fields[columnIndex["coreg"]] != "" || fields[columnIndex["segments"]] != "" {
			continue // subsidiary or dimensionally-broken-down row, not the consolidated total
		}
		if fields[columnIndex["qtrs"]] != rule.qtrs || fields[columnIndex["uom"]] != rule.uom {
			continue
		}

		ddate, err := time.Parse(dateLayout, fields[columnIndex["ddate"]])
		if err != nil {
			continue
		}
		value, err := strconv.ParseFloat(fields[columnIndex["value"]], 64)
		if err != nil {
			continue // blank or malformed value column
		}

		if facts[adsh] == nil {
			facts[adsh] = make(map[string][]dateValue)
		}
		facts[adsh][rule.tag] = append(facts[adsh][rule.tag], dateValue{ddate: ddate, value: value})
	}
	if err := scanner.Err(); err != nil {
		return nil, err
	}

	return facts, nil
}

// buildFundamentals selects the current and, where needed, prior-year value
// for each field from one company's tagFacts (as returned by readNumFacts).
func buildFundamentals(tagFacts map[string][]dateValue, period time.Time) Fundamentals {
	var f Fundamentals
	f.Assets, _ = firstMatch(tagFacts, period, false, "Assets")
	f.Liabilities, _ = firstMatch(tagFacts, period, false, "Liabilities")
	f.StockholdersEquity, _ = firstMatch(tagFacts, period, false, "StockholdersEquity")
	f.NetIncomeLoss, f.NetIncomeLossPrior = firstMatch(tagFacts, period, true, "NetIncomeLoss")
	f.OperatingIncomeLoss, _ = firstMatch(tagFacts, period, false, "OperatingIncomeLoss")
	f.OperatingCashFlow, f.OperatingCashFlowPrior = firstMatch(tagFacts, period, true, "NetCashProvidedByUsedInOperatingActivities")
	f.Capex, f.CapexPrior = firstMatch(tagFacts, period, true, "PaymentsForCapitalExpenditures", "PaymentsToAcquirePropertyPlantAndEquipment")
	f.Revenue, _ = firstMatch(tagFacts, period, false, "Revenues", "RevenueFromContractWithCustomerExcludingAssessedTax")
	f.Cash, _ = firstMatch(tagFacts, period, false, "CashAndCashEquivalentsAtCarryingValue")
	f.LongTermDebt, _ = firstMatch(tagFacts, period, false, "LongTermDebtNoncurrent")
	f.SharesOutstanding, _ = firstMatch(tagFacts, period, false, "CommonStockSharesOutstanding", "EntityCommonStockSharesOutstanding")
	return f
}

// firstMatch tries each candidate tag in priority order and returns the
// current/prior values from the first one that has a value for the current
// fiscal period (ddate == period). A later candidate is only used when an
// earlier-priority tag has no current-year value at all, never to mix a
// current value from one tag with a prior value from another.
func firstMatch(tagFacts map[string][]dateValue, period time.Time, needsPrior bool, tags ...string) (current, prior *float64) {
	for _, tag := range tags {
		if current, prior = selectCurrentAndPrior(tagFacts[tag], period, needsPrior); current != nil {
			return current, prior
		}
	}
	return nil, nil
}

// selectCurrentAndPrior finds the value at ddate == period (current) and,
// if needsPrior, the value at the closest ddate strictly before it (the
// second-most-recent distinct ddate, i.e. the prior fiscal year's
// comparative figure from the same filing).
func selectCurrentAndPrior(entries []dateValue, period time.Time, needsPrior bool) (current, prior *float64) {
	found := false
	var currentVal float64
	for _, e := range entries {
		if e.ddate.Equal(period) {
			currentVal, found = e.value, true
			break
		}
	}
	if !found {
		return nil, nil
	}
	current = &currentVal
	if !needsPrior {
		return current, nil
	}

	havePrior := false
	var priorVal float64
	var priorDDate time.Time
	for _, e := range entries {
		if e.ddate.Before(period) && (!havePrior || e.ddate.After(priorDDate)) {
			priorDDate, priorVal, havePrior = e.ddate, e.value, true
		}
	}
	if havePrior {
		prior = &priorVal
	}
	return current, prior
}
