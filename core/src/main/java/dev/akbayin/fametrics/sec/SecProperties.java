package dev.akbayin.fametrics.sec;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sec")
public record SecProperties(String userAgentEmail) {
}
