package com.uyinvest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PortfolioRequest(

        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 150, message = "Name must be between 2 and 150 characters")
        String name,

        @Size(max = 1000, message = "Description must be at most 1000 characters")
        String description,

        @NotBlank(message = "Base currency is required")
        @Pattern(regexp = "^[A-Z]{3}$", message = "Base currency must be a 3-letter ISO 4217 code (e.g. USD)")
        String baseCurrency) {
}
