package com.uyinvest.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record DividendResponse(
        UUID id,
        AssetResponse asset,
        BigDecimal amount,
        String currency,
        LocalDate paymentDate,
        Instant createdAt) {
}
