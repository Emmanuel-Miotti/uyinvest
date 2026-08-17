package com.uyinvest.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record GoalResponse(
        UUID id,
        String name,
        BigDecimal targetAmount,
        BigDecimal currentAmount,
        String currency,
        LocalDate targetDate,
        BigDecimal progressPercentage,
        Instant createdAt,
        Instant updatedAt) {
}
