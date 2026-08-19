package com.uyinvest.controller;

import com.uyinvest.dto.request.DividendRequest;
import com.uyinvest.dto.response.DividendResponse;
import com.uyinvest.dto.response.DividendSummaryResponse;
import com.uyinvest.exception.ErrorResponse;
import com.uyinvest.security.CustomUserDetails;
import com.uyinvest.service.DividendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/portfolios/{portfolioId}/dividends")
@RequiredArgsConstructor
@Tag(name = "Dividends", description = "Dividend payments recorded against a portfolio")
public class DividendController {

    private final DividendService dividendService;

    @GetMapping
    @Operation(summary = "List a portfolio's dividends", description = "Optional filters by asset and payment date range")
    @ApiResponse(responseCode = "404", description = "Portfolio not found or not owned by the caller",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<List<DividendResponse>> getAll(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID portfolioId,
            @Parameter(description = "Filter by asset id") @RequestParam(required = false) UUID assetId,
            @Parameter(description = "Payment date lower bound (inclusive)") @RequestParam(required = false) LocalDate from,
            @Parameter(description = "Payment date upper bound (inclusive)") @RequestParam(required = false) LocalDate to) {
        return ResponseEntity.ok(dividendService.getAllForPortfolio(principal.getId(), portfolioId, assetId, from, to));
    }

    @PostMapping
    @Operation(summary = "Register a dividend payment")
    @ApiResponse(responseCode = "404", description = "Portfolio or asset not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<DividendResponse> create(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID portfolioId,
            @Valid @RequestBody DividendRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dividendService.create(principal.getId(), portfolioId, request));
    }

    @GetMapping("/summary")
    @Operation(summary = "Get total dividends for this month, this year, and historically")
    @ApiResponse(responseCode = "404", description = "Portfolio not found or not owned by the caller",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<DividendSummaryResponse> getSummary(
            @AuthenticationPrincipal CustomUserDetails principal, @PathVariable UUID portfolioId) {
        return ResponseEntity.ok(dividendService.getSummary(principal.getId(), portfolioId));
    }
}
