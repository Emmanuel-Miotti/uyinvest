package com.uyinvest.dto.response;

import java.time.Instant;
import java.util.UUID;

public record PortfolioResponse(
        UUID id,
        String name,
        String description,
        String baseCurrency,
        Instant createdAt,
        Instant updatedAt) {
}
