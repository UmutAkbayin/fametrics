package dev.akbayin.fametrics.sec;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SecTickerClientTest {

    // A trimmed real sample of SEC's own file shape: an object keyed by
    // stringified indices, values with cik_str/ticker/title.
    private static final String TICKERS_JSON = """
        {
          "0": {"cik_str": 1045810, "ticker": "NVDA", "title": "NVIDIA CORP"},
          "1": {"cik_str": 320193, "ticker": "AAPL", "title": "Apple Inc."}
        }
        """;

    @Test
    void fetchTickerEntries_sendsSecRequiredUserAgentAndParsesSnakeCaseJson() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SecTickerClient client = new SecTickerClient(builder, new SecProperties("test@example.com"));

        server.expect(requestTo("https://www.sec.gov/files/company_tickers.json"))
            .andExpect(header(HttpHeaders.USER_AGENT, "Fametrics-Core/1.0 (test@example.com)"))
            .andRespond(withSuccess(TICKERS_JSON, MediaType.APPLICATION_JSON));

        Map<String, SecTickerEntry> got = client.fetchTickerEntries();

        assertThat(got).hasSize(2);
        assertThat(got.get("0")).isEqualTo(new SecTickerEntry(1045810L, "NVDA", "NVIDIA CORP"));
        assertThat(got.get("1")).isEqualTo(new SecTickerEntry(320193L, "AAPL", "Apple Inc."));

        server.verify();
    }
}
