package com.uyinvest.service.marketdata;

import com.uyinvest.exception.MarketDataUnavailableException;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
@ConditionalOnProperty(prefix = "market-data", name = "provider", havingValue = "external")
public class ExternalMarketDataProvider implements MarketDataProvider {

    private final RestClient restClient;
    private final String apiKey;

    public ExternalMarketDataProvider(
            RestClient.Builder restClientBuilder,
            @Value("${market-data.base-url}") String baseUrl,
            @Value("${market-data.api-key}") String apiKey) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
    }

    @Override
    public BigDecimal getCurrentPrice(String symbol) {
        FinnhubQuoteResponse quote;
        try {
            quote = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/quote")
                            .queryParam("symbol", symbol)
                            .queryParam("token", apiKey)
                            .build())
                    .retrieve()
                    .body(FinnhubQuoteResponse.class);
        } catch (RestClientException e) {
            throw new MarketDataUnavailableException("Failed to fetch price for symbol: " + symbol, e);
        }

        if (quote == null || quote.currentPrice() == null || quote.currentPrice().signum() <= 0) {
            throw new MarketDataUnavailableException("No price available for symbol: " + symbol);
        }

        return quote.currentPrice();
    }

    private record FinnhubQuoteResponse(BigDecimal c) {
        BigDecimal currentPrice() {
            return c;
        }
    }
}
