package com.uyinvest.dto.response;

import com.uyinvest.entity.enums.AssetType;
import java.time.Instant;
import java.util.UUID;

public record AssetResponse(
        UUID id,
        String symbol,
        String name,
        AssetType type,
        String currency,
        String sector,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {
}
