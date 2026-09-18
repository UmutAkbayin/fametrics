package dev.akbayin.fametrics.screener;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "screener")
public record ScreenerProperties(String baseUrl) {
}
