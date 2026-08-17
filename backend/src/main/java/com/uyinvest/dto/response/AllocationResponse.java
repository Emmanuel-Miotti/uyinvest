package com.uyinvest.dto.response;

import com.uyinvest.entity.enums.AssetType;
import java.math.BigDecimal;

public record AllocationResponse(
        AssetType assetType,
        BigDecimal currentValue,
        BigDecimal percentage) {
}
