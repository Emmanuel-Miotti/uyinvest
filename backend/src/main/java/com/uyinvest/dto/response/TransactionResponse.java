package com.uyinvest.dto.response;

import com.uyinvest.entity.enums.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        AssetResponse asset,
        TransactionType type,
        BigDecimal quantity,
        BigDecimal price,
        BigDecimal commission,
        String currency,
        Instant transactionDate,
        Instant createdAt) {
}
