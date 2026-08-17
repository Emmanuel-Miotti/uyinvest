package com.uyinvest.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record PerformancePointResponse(
        Instant date,
        BigDecimal totalInvested) {
}
