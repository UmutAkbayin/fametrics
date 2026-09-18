package dev.akbayin.fametrics.sec;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * One entry of SEC's {@code company_tickers.json}. The file itself is a JSON
 * object keyed by stringified indices ("0", "1", ...), not an array — see
 * {@link SecTickerClient}.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record SecTickerEntry(Long cikStr, String ticker, String title) {
}
