package com.uyinvest.dto.response;

import java.math.BigDecimal;

public record DividendSummaryResponse(
        BigDecimal totalThisMonth,
        BigDecimal totalThisYear,
        BigDecimal totalHistorical) {
}
