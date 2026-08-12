package dev.akbayin.fametrics.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI faMetricsOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("fa-metrics API")
                .description("REST API for computing common stock valuation metrics from raw financial inputs.")
                .version("0.0.1"));
    }
}