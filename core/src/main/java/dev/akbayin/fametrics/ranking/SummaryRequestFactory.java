package dev.akbayin.fametrics.ranking;

import dev.akbayin.fametrics.dto.CapitalStructure;
import dev.akbayin.fametrics.dto.FundamentalData;
import dev.akbayin.fametrics.dto.MarketData;
import dev.akbayin.fametrics.dto.SummaryRequest;
import dev.akbayin.fametrics.pricing.CachedPrice;
import dev.akbayin.fametrics.screener.ScreenerCandidate;

/**
 * Builds the existing engine's {@link SummaryRequest} from a screener
 * candidate's price-independent fundamentals combined with its cached FMP
 * price data. Pure field mapping, no Spring wiring needed, so it's a static
 * factory rather than a bean.
 * <p>
 * eps/bvps come from the screener (price-independent, computed once at
 * ingestion time), not FMP — FMP only supplies sharePrice and marketCap
 * here.
 */
public final class SummaryRequestFactory {

    private SummaryRequestFactory() {
    }

    public static SummaryRequest from(ScreenerCandidate candidate, CachedPrice price) {
        var marketData = new MarketData(price.price(), candidate.eps(), candidate.bvps());
        var fundamentalData = new FundamentalData(price.marketCap(), candidate.revenue(), candidate.epsGrowthRate());
        var capitalStructure = new CapitalStructure(candidate.liabilities(), candidate.stockholdersEquity(), candidate.netIncome());
        return new SummaryRequest(marketData, fundamentalData, capitalStructure);
    }
}
