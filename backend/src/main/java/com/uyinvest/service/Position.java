package com.uyinvest.service;

import java.math.BigDecimal;

public record Position(
        BigDecimal quantity,
        BigDecimal costBasis,
        BigDecimal averagePrice,
        BigDecimal currentPrice,
        BigDecimal currentValue,
        BigDecimal profitLoss,
        BigDecimal profitLossPercentage) {
}
