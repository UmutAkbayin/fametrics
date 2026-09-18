package dev.akbayin.fametrics.screener;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ScreenerClientTest {

    // A real response captured from the screener during development, so the
    // fixture matches its actual shape rather than an idealized one — note
    // long_term_debt: null, exactly the "tag not reported" case the
    // screener's own docs call out.
    private static final String CANDIDATE_JSON = """
        [
          {
            "cik": 1054102,
            "name": "INTERPACE BIOSCIENCES, INC.",
            "period_end": "2025-12-31",
            "adsh": "0001493152-26-013646",
            "assets": 33838000,
            "liabilities": 11475000,
            "stockholders_equity": 22363000,
            "net_income": 24575000,
            "operating_income": 4084000,
            "operating_cash_flow": 5831000,
            "capex": 356000,
            "revenue": 38728000,
            "cash": 2505000,
            "long_term_debt": null,
            "shares_outstanding": 4428539,
            "operating_cash_flow_prior": 4646000,
            "capex_prior": 876000,
            "net_income_prior": 6702000,
            "eps": 5.5492,
            "bvps": 5.0497,
            "eps_growth_rate": 2.6668,
            "quality_score": 100,
            "passes_hard_filter": true,
            "updated_at": "2026-09-18T10:20:57.919101+02:00"
          }
        ]
        """;

    private ScreenerClient clientBoundTo(MockRestServiceServer[] serverOut) {
        RestClient.Builder builder = RestClient.builder();
        serverOut[0] = MockRestServiceServer.bindTo(builder).build();
        return new ScreenerClient(builder, new ScreenerProperties("http://screener.test"));
    }

    @Test
    void fetchCandidates_parsesSnakeCaseJsonIntoCamelCaseFields() {
        MockRestServiceServer[] serverHolder = new MockRestServiceServer[1];
        ScreenerClient client = clientBoundTo(serverHolder);
        serverHolder[0].expect(requestTo("http://screener.test/candidates?limit=200"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(CANDIDATE_JSON, MediaType.APPLICATION_JSON));

        List<ScreenerCandidate> got = client.fetchCandidates(200);

        assertThat(got).hasSize(1);
        ScreenerCandidate c = got.get(0);
        assertThat(c.cik()).isEqualTo(1054102L);
        assertThat(c.name()).isEqualTo("INTERPACE BIOSCIENCES, INC.");
        assertThat(c.periodEnd()).isEqualTo(LocalDate.of(2025, 12, 31));
        assertThat(c.adsh()).isEqualTo("0001493152-26-013646");
        assertThat(c.assets()).isEqualByComparingTo(new BigDecimal("33838000"));
        assertThat(c.longTermDebt()).isNull();
        assertThat(c.eps()).isEqualByComparingTo(new BigDecimal("5.5492"));
        assertThat(c.qualityScore()).isEqualByComparingTo(new BigDecimal("100"));
        assertThat(c.passesHardFilter()).isTrue();
        assertThat(c.updatedAt()).isEqualTo(OffsetDateTime.parse("2026-09-18T10:20:57.919101+02:00"));

        serverHolder[0].verify();
    }

    @Test
    void fetchCandidates_emptyArrayReturnsEmptyListNotNull() {
        MockRestServiceServer[] serverHolder = new MockRestServiceServer[1];
        ScreenerClient client = clientBoundTo(serverHolder);
        serverHolder[0].expect(requestTo("http://screener.test/candidates?limit=5"))
            .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        List<ScreenerCandidate> got = client.fetchCandidates(5);

        assertThat(got).isNotNull().isEmpty();
    }
}
