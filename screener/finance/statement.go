package finance

import (
	"cmp"
	"fmt"
	"io"
	"net/http"
	"os"
	"regexp"
	"slices"
	"strings"
)

func getWithUA(url string) (*http.Response, error) {
	contact := os.Getenv("SEC_USER_AGENT_EMAIL")
	if contact == "" {
		return nil, fmt.Errorf("SEC_USER_AGENT_EMAIL environment variable is not set; SEC requires a contact address in the User-Agent header")
	}

	req, err := http.NewRequest("GET", url, nil)
	if err != nil {
		return nil, err
	}
	req.Header.Set("User-Agent", fmt.Sprintf("Fametrics-Screener/1.0 (%s)", contact))
	return http.DefaultClient.Do(req)
}

// zipLinkPattern matches href values like ".../2026q2.zip".
var zipLinkPattern = regexp.MustCompile(`href=["']([^"']*/(\d{4})q([1-4])\.zip)["']`)

// FindLatestZipURLs returns up to n of the newest SEC financial statement zip
// URLs listed on pageURL, ordered newest to oldest. It returns fewer than n
// entries if the page lists fewer matching zips.
func FindLatestZipURLs(pageURL string, n int) ([]string, error) {
	resp, err := getWithUA(pageURL)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("HTTP error while loading the page: %s", resp.Status)
	}

	htmlBytes, err := io.ReadAll(resp.Body)
	if err != nil {
		return nil, err
	}

	matches := zipLinkPattern.FindAllStringSubmatch(string(htmlBytes), -1)
	if len(matches) == 0 {
		return nil, fmt.Errorf("no matching ZIP files found")
	}

	// Newest first by year then quarter. Both are fixed-width numeric strings
	// (4-digit year, 1-digit quarter), so lexicographic comparison matches
	// numeric order.
	slices.SortFunc(matches, func(a, b []string) int {
		if c := strings.Compare(b[2], a[2]); c != 0 {
			return c
		}
		return cmp.Compare(b[3], a[3])
	})

	count := min(n, len(matches))
	res := make([]string, count)
	for i := 0; i < count; i++ {
		fullLink := matches[i][1]
		if strings.HasPrefix(fullLink, "/") {
			fullLink = "https://www.sec.gov" + fullLink
		}
		res[i] = fullLink
	}

	return res, nil
}

func DownloadFile(url, destPath string) error {
	resp, err := getWithUA(url)
	if err != nil {
		return err
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return fmt.Errorf("download failed: %s", resp.Status)
	}

	out, err := os.Create(destPath)
	if err != nil {
		return err
	}
	defer out.Close()

	_, err = io.Copy(out, resp.Body)
	return err
}
