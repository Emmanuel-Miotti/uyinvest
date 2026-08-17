package com.uyinvest.service.marketdata;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

// Deterministic placeholder price source, active while market-data.provider=mock (the default).
@Service
@ConditionalOnProperty(prefix = "market-data", name = "provider", havingValue = "mock", matchIfMissing = true)
public class MockMarketDataProvider implements MarketDataProvider {

    @Override
    public BigDecimal getCurrentPrice(String symbol) {
        int base = Math.floorMod(symbol.hashCode(), 400) + 50;
        return BigDecimal.valueOf(base).setScale(2, RoundingMode.HALF_UP);
    }
}
