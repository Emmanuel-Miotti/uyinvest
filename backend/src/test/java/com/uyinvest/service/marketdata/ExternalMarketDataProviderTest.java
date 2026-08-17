package com.uyinvest.service.marketdata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.uyinvest.exception.MarketDataUnavailableException;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class ExternalMarketDataProviderTest {

    private MockRestServiceServer mockServer;
    private ExternalMarketDataProvider provider;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        provider = new ExternalMarketDataProvider(builder, "https://finnhub.io/api/v1", "test-api-key");
    }

    @Test
    void returnsCurrentPriceFromQuoteResponse() {
        mockServer.expect(requestTo(containsString("/quote?symbol=AAPL&token=test-api-key")))
                .andRespond(withSuccess("{\"c\": 150.25, \"h\": 151, \"l\": 149, \"o\": 150, \"pc\": 149.5}", MediaType.APPLICATION_JSON));

        BigDecimal price = provider.getCurrentPrice("AAPL");

        assertThat(price).isEqualByComparingTo("150.25");
    }

    @Test
    void throwsWhenSymbolIsUnknown() {
        mockServer.expect(requestTo(containsString("symbol=UNKNOWN")))
                .andRespond(withSuccess("{\"c\": 0, \"h\": 0, \"l\": 0, \"o\": 0, \"pc\": 0}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> provider.getCurrentPrice("UNKNOWN"))
                .isInstanceOf(MarketDataUnavailableException.class);
    }

    @Test
    void throwsWhenApiCallFails() {
        mockServer.expect(requestTo(containsString("symbol=AAPL")))
                .andRespond(withServerError());

        assertThatThrownBy(() -> provider.getCurrentPrice("AAPL"))
                .isInstanceOf(MarketDataUnavailableException.class);
    }
}
