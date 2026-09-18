package dev.akbayin.fametrics.fmp;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class FmpClientTest {

    // A real /stable/profile response for AZZ, trimmed to the fields we
    // actually parse plus a few of the ~20 we deliberately ignore, to prove
    // ignoreUnknown actually works rather than just having no unknown
    // fields to trip over.
    private static final String AZZ_PROFILE_JSON = """
        [
          {
            "symbol": "AZZ",
            "price": 133.66,
            "marketCap": 4016683490,
            "companyName": "AZZ Inc.",
            "cik": "0000008947",
            "description": "AZZ Inc. specializes in..."
          }
        ]
        """;

    private FmpClient client;
    private MockRestServiceServer server;

    private void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new FmpClient(builder, new FmpProperties("test-key"));
    }

    @Test
    void fetchProfile_parsesKnownFieldsAndIgnoresUnknownOnes() {
        setUp();
        server.expect(requestTo("https://financialmodelingprep.com/stable/profile?symbol=AZZ&apikey=test-key"))
            .andRespond(withSuccess(AZZ_PROFILE_JSON, MediaType.APPLICATION_JSON));

        Optional<FmpProfile> got = client.fetchProfile("AZZ");

        assertThat(got).contains(new FmpProfile("AZZ", new BigDecimal("133.66"), new BigDecimal("4016683490")));
        server.verify();
    }

    @Test
    void fetchProfile_emptyArrayMeansUnknownSymbol_returnsEmpty() {
        setUp();
        server.expect(requestTo("https://financialmodelingprep.com/stable/profile?symbol=ZZZZNOTREAL&apikey=test-key"))
            .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        Optional<FmpProfile> got = client.fetchProfile("ZZZZNOTREAL");

        assertThat(got).isEmpty();
    }
}
