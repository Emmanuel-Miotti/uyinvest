package com.uyinvest.dto.request;

import com.uyinvest.entity.enums.AssetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AssetRequest(

        @NotBlank(message = "Symbol is required")
        @Size(max = 20, message = "Symbol must be at most 20 characters")
        String symbol,

        @NotBlank(message = "Name is required")
        @Size(max = 150, message = "Name must be at most 150 characters")
        String name,

        @NotNull(message = "Type is required")
        AssetType type,

        @NotBlank(message = "Currency is required")
        @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter ISO 4217 code (e.g. USD)")
        String currency,

        @Size(max = 100, message = "Sector must be at most 100 characters")
        String sector,

        @NotNull(message = "Active flag is required")
        Boolean active) {
}
