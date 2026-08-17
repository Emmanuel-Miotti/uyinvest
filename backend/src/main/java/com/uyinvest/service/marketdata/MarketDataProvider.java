package com.uyinvest.service.marketdata;

import java.math.BigDecimal;

public interface MarketDataProvider {

    BigDecimal getCurrentPrice(String symbol);
}
