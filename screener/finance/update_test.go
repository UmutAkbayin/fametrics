package finance

import (
	"net/http"
	"net/http/httptest"
	"os"
	"path/filepath"
	"testing"
)

func TestUpdateLatestStatements_DownloadsWhenMissing(t *testing.T) {
	t.Setenv("SEC_USER_AGENT_EMAIL", "test@example.com")

	const zipContent = "fake zip content"
	var downloadCount int
	var server *httptest.Server
	server = httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path == "/files/2026q2.zip" {
			downloadCount++
			w.Write([]byte(zipContent))
			return
		}
		w.Write([]byte(`<a href="` + server.URL + `/files/2026q2.zip">2026 Q2</a>`))
	}))
	defer server.Close()

	dir := t.TempDir()
	outFile, err := UpdateLatestStatements(server.URL, dir)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}

	wantPath := filepath.Join(dir, "2026q2.zip")
	if outFile != wantPath {
		t.Errorf("got path %q, want %q", outFile, wantPath)
	}
	if downloadCount != 1 {
		t.Errorf("expected exactly 1 download, got %d", downloadCount)
	}

	got, err := os.ReadFile(outFile)
	if err != nil {
		t.Fatalf("failed to read downloaded file: %v", err)
	}
	if string(got) != zipContent {
		t.Errorf("got content %q, want %q", got, zipContent)
	}
}

func TestUpdateLatestStatements_SkipsWhenAlreadyPresent(t *testing.T) {
	t.Setenv("SEC_USER_AGENT_EMAIL", "test@example.com")

	var downloadCount int
	var server *httptest.Server
	server = httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path == "/files/2026q2.zip" {
			downloadCount++
			w.Write([]byte("should not be fetched"))
			return
		}
		w.Write([]byte(`<a href="` + server.URL + `/files/2026q2.zip">2026 Q2</a>`))
	}))
	defer server.Close()

	dir := t.TempDir()
	existingPath := filepath.Join(dir, "2026q2.zip")
	const existingContent = "already downloaded"
	if err := os.WriteFile(existingPath, []byte(existingContent), 0o644); err != nil {
		t.Fatalf("failed to seed existing file: %v", err)
	}

	outFile, err := UpdateLatestStatements(server.URL, dir)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}

	if outFile != existingPath {
		t.Errorf("got path %q, want %q", outFile, existingPath)
	}
	if downloadCount != 0 {
		t.Errorf("expected no download when file already exists, got %d", downloadCount)
	}

	got, err := os.ReadFile(existingPath)
	if err != nil {
		t.Fatalf("failed to read file: %v", err)
	}
	if string(got) != existingContent {
		t.Errorf("existing file was overwritten: got %q, want %q", got, existingContent)
	}
}

func TestUpdateLatestStatements_PropagatesFindError(t *testing.T) {
	t.Setenv("SEC_USER_AGENT_EMAIL", "test@example.com")

	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusInternalServerError)
	}))
	defer server.Close()

	if _, err := UpdateLatestStatements(server.URL, t.TempDir()); err == nil {
		t.Fatal("expected an error when the page can't be fetched")
	}
}
