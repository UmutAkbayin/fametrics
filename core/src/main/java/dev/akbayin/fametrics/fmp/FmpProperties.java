package dev.akbayin.fametrics.fmp;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fmp")
public record FmpProperties(String apiKey) {
}
