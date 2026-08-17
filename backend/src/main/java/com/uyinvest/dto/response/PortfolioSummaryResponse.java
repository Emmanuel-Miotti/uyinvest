package com.uyinvest.dto.response;

import java.math.BigDecimal;

public record PortfolioSummaryResponse(
        BigDecimal totalInvested,
        BigDecimal currentValue,
        BigDecimal profitLoss,
        BigDecimal profitLossPercentage) {
}
