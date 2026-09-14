package finance

import (
	"net/http"
	"net/http/httptest"
	"os"
	"path/filepath"
	"slices"
	"testing"
)

func TestFindLatestZipURLs_OrdersNewestFirst(t *testing.T) {
	t.Setenv("SEC_USER_AGENT_EMAIL", "test@example.com")

	var gotUA string
	var server *httptest.Server
	server = httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		gotUA = r.Header.Get("User-Agent")
		w.Write([]byte(`
			<a href="` + server.URL + `/files/2023q1.zip">2023 Q1</a>
			<a href="` + server.URL + `/files/2026q2.zip">2026 Q2</a>
			<a href="` + server.URL + `/files/2025q4.zip">2025 Q4</a>
		`))
	}))
	defer server.Close()

	got, err := FindLatestZipURLs(server.URL, 3)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}

	want := []string{
		server.URL + "/files/2026q2.zip",
		server.URL + "/files/2025q4.zip",
		server.URL + "/files/2023q1.zip",
	}
	if !slices.Equal(got, want) {
		t.Errorf("got %v, want %v", got, want)
	}
	if gotUA == "" {
		t.Error("expected a non-empty User-Agent header to be sent")
	}
}

func TestFindLatestZipURLs_FewerMatchesThanRequested(t *testing.T) {
	t.Setenv("SEC_USER_AGENT_EMAIL", "test@example.com")

	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Write([]byte(`<a href="/files/2025q4.zip">2025 Q4</a>`))
	}))
	defer server.Close()

	got, err := FindLatestZipURLs(server.URL, 4)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}

	want := []string{"https://www.sec.gov/files/2025q4.zip"}
	if !slices.Equal(got, want) {
		t.Errorf("got %v, want %v", got, want)
	}
}

func TestFindLatestZipURLs_NoMatches(t *testing.T) {
	t.Setenv("SEC_USER_AGENT_EMAIL", "test@example.com")

	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Write([]byte(`<html><body>nothing here</body></html>`))
	}))
	defer server.Close()

	if _, err := FindLatestZipURLs(server.URL, 4); err == nil {
		t.Fatal("expected an error when no zip links are found")
	}
}

func TestFindLatestZipURLs_HTTPError(t *testing.T) {
	t.Setenv("SEC_USER_AGENT_EMAIL", "test@example.com")

	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusInternalServerError)
	}))
	defer server.Close()

	if _, err := FindLatestZipURLs(server.URL, 4); err == nil {
		t.Fatal("expected an error on non-200 response")
	}
}

func TestFindLatestZipURLs_MissingUserAgentEnv(t *testing.T) {
	t.Setenv("SEC_USER_AGENT_EMAIL", "")

	if _, err := FindLatestZipURLs("http://example.com", 4); err == nil {
		t.Fatal("expected an error when SEC_USER_AGENT_EMAIL is unset")
	}
}

func TestDownloadFile_Success(t *testing.T) {
	t.Setenv("SEC_USER_AGENT_EMAIL", "test@example.com")

	const content = "fake zip content"
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Write([]byte(content))
	}))
	defer server.Close()

	dest := filepath.Join(t.TempDir(), "out.zip")
	if err := DownloadFile(server.URL, dest); err != nil {
		t.Fatalf("unexpected error: %v", err)
	}

	got, err := os.ReadFile(dest)
	if err != nil {
		t.Fatalf("failed to read downloaded file: %v", err)
	}
	if string(got) != content {
		t.Errorf("got %q, want %q", got, content)
	}
}

func TestDownloadFile_HTTPError(t *testing.T) {
	t.Setenv("SEC_USER_AGENT_EMAIL", "test@example.com")

	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusNotFound)
	}))
	defer server.Close()

	dest := filepath.Join(t.TempDir(), "out.zip")
	if err := DownloadFile(server.URL, dest); err == nil {
		t.Fatal("expected an error on non-200 response")
	}
}
