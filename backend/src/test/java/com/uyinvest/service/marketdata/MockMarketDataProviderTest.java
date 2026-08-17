package com.uyinvest.service.marketdata;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MockMarketDataProviderTest {

    private final MockMarketDataProvider provider = new MockMarketDataProvider();

    @Test
    void returnsSamePriceForSameSymbolAcrossCalls() {
        assertThat(provider.getCurrentPrice("AAPL")).isEqualByComparingTo(provider.getCurrentPrice("AAPL"));
    }

    @Test
    void returnsPositivePriceForAnySymbol() {
        assertThat(provider.getCurrentPrice("AAPL").signum()).isPositive();
        assertThat(provider.getCurrentPrice("X").signum()).isPositive();
    }

    @Test
    void differentSymbolsTendToGetDifferentPrices() {
        assertThat(provider.getCurrentPrice("AAPL")).isNotEqualByComparingTo(provider.getCurrentPrice("MSFT"));
    }
}
