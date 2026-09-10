package finance

import (
	"fmt"
	"io"
	"net/http"
	"os"
	"regexp"
	"strconv"
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

func FindLatestZipURL(pageURL string) (string, error) {
	resp, err := getWithUA(pageURL)
	if err != nil {
		return "", err
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return "", fmt.Errorf("HTTP error while loading the page: %s", resp.Status)
	}

	htmlBytes, err := io.ReadAll(resp.Body)
	if err != nil {
		return "", err
	}
	html := string(htmlBytes)

	// Regex looking for strings like ".../2026q2.zip"
	re := regexp.MustCompile(`href=["']([^"']*/(\d{4})q([1-4])\.zip)["']`)
	matches := re.FindAllStringSubmatch(html, -1)

	if len(matches) == 0 {
		return "", fmt.Errorf("keine passenden ZIP-Dateien gefunden")
	}

	var latestURL string
	var maxScore int

	for _, match := range matches {
		fullLink := match[1]
		year, _ := strconv.Atoi(match[2])
		quarter, _ := strconv.Atoi(match[3])
		score := year*10 + quarter

		if score > maxScore {
			maxScore = score
			latestURL = fullLink
		}
	}

	if len(latestURL) > 0 && latestURL[0] == '/' {
		latestURL = "https://www.sec.gov" + latestURL
	}

	return latestURL, nil
}

func DownloadFile(url, destPath string) error {
	resp, err := getWithUA(url)
	if err != nil {
		return err
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return fmt.Errorf("download fehlgeschlagen: %s", resp.Status)
	}

	out, err := os.Create(destPath)
	if err != nil {
		return err
	}
	defer out.Close()

	_, err = io.Copy(out, resp.Body)
	return err
}
