package finance

import (
	"fmt"
	"net/http"
	"net/http/httptest"
	"os"
	"path/filepath"
	"testing"
)

func TestUpdateLatestStatements_DownloadsMissingAndSkipsExisting(t *testing.T) {
	t.Setenv("SEC_USER_AGENT_EMAIL", "test@example.com")

	downloadCount := map[string]int{}
	var server *httptest.Server
	server = httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		switch r.URL.Path {
		case "/files/2026q2.zip", "/files/2025q4.zip", "/files/2025q3.zip":
			downloadCount[r.URL.Path]++
			fmt.Fprintf(w, "content for %s", r.URL.Path)
		default:
			w.Write([]byte(`
				<a href="` + server.URL + `/files/2026q2.zip">2026 Q2</a>
				<a href="` + server.URL + `/files/2025q4.zip">2025 Q4</a>
				<a href="` + server.URL + `/files/2025q3.zip">2025 Q3</a>
			`))
		}
	}))
	defer server.Close()

	dir := t.TempDir()
	const existingContent = "already downloaded"
	existingPath := filepath.Join(dir, "2025q4.zip")
	if err := os.WriteFile(existingPath, []byte(existingContent), 0o644); err != nil {
		t.Fatalf("failed to seed existing file: %v", err)
	}

	if err := UpdateLatestStatements(server.URL, dir); err != nil {
		t.Fatalf("unexpected error: %v", err)
	}

	if downloadCount["/files/2025q4.zip"] != 0 {
		t.Errorf("expected the already-present file not to be re-downloaded, got %d fetches", downloadCount["/files/2025q4.zip"])
	}
	if got, err := os.ReadFile(existingPath); err != nil || string(got) != existingContent {
		t.Errorf("existing file was overwritten: got %q, err %v", got, err)
	}

	for _, name := range []string{"2026q2.zip", "2025q3.zip"} {
		if downloadCount["/files/"+name] != 1 {
			t.Errorf("expected exactly 1 download for %s, got %d", name, downloadCount["/files/"+name])
		}
		got, err := os.ReadFile(filepath.Join(dir, name))
		if err != nil {
			t.Fatalf("failed to read %s: %v", name, err)
		}
		want := "content for /files/" + name
		if string(got) != want {
			t.Errorf("got content %q, want %q", got, want)
		}
	}
}

func TestUpdateLatestStatements_CreatesMissingResourcesDirectory(t *testing.T) {
	t.Setenv("SEC_USER_AGENT_EMAIL", "test@example.com")

	var server *httptest.Server
	server = httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path == "/files/2026q2.zip" {
			w.Write([]byte("zip content"))
			return
		}
		w.Write([]byte(`<a href="` + server.URL + `/files/2026q2.zip">2026 Q2</a>`))
	}))
	defer server.Close()

	dir := filepath.Join(t.TempDir(), "nested", "resources")
	if err := UpdateLatestStatements(server.URL, dir); err != nil {
		t.Fatalf("unexpected error: %v", err)
	}

	if _, err := os.Stat(filepath.Join(dir, "2026q2.zip")); err != nil {
		t.Errorf("expected file to exist in newly created directory: %v", err)
	}
}

func TestUpdateLatestStatements_ContinuesAfterOneDownloadFails(t *testing.T) {
	t.Setenv("SEC_USER_AGENT_EMAIL", "test@example.com")

	var server *httptest.Server
	server = httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		switch r.URL.Path {
		case "/files/2026q2.zip":
			w.WriteHeader(http.StatusInternalServerError)
		case "/files/2025q4.zip":
			w.Write([]byte("zip content"))
		default:
			w.Write([]byte(`
				<a href="` + server.URL + `/files/2026q2.zip">2026 Q2</a>
				<a href="` + server.URL + `/files/2025q4.zip">2025 Q4</a>
			`))
		}
	}))
	defer server.Close()

	dir := t.TempDir()
	if err := UpdateLatestStatements(server.URL, dir); err != nil {
		t.Fatalf("expected a single failed download not to fail the whole update: %v", err)
	}

	if _, err := os.Stat(filepath.Join(dir, "2025q4.zip")); err != nil {
		t.Errorf("expected the succeeding download to be saved: %v", err)
	}
	if _, err := os.Stat(filepath.Join(dir, "2026q2.zip")); err == nil {
		t.Error("expected the failing download not to leave a file behind")
	}
}

func TestUpdateLatestStatements_PropagatesFindError(t *testing.T) {
	t.Setenv("SEC_USER_AGENT_EMAIL", "test@example.com")

	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusInternalServerError)
	}))
	defer server.Close()

	if err := UpdateLatestStatements(server.URL, t.TempDir()); err == nil {
		t.Fatal("expected an error when the page can't be fetched")
	}
}
