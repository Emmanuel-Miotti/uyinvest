package com.uyinvest.controller;

import com.uyinvest.dto.request.TransactionRequest;
import com.uyinvest.dto.response.TransactionResponse;
import com.uyinvest.exception.ErrorResponse;
import com.uyinvest.security.CustomUserDetails;
import com.uyinvest.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/portfolios/{portfolioId}/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Buy/sell operations within a portfolio")
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping
    @Operation(summary = "List a portfolio's transactions, most recent first")
    @ApiResponse(responseCode = "404", description = "Portfolio not found or not owned by the caller",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<List<TransactionResponse>> getAll(
            @AuthenticationPrincipal CustomUserDetails principal, @PathVariable UUID portfolioId) {
        return ResponseEntity.ok(transactionService.getAllForPortfolio(principal.getId(), portfolioId));
    }

    @PostMapping
    @Operation(summary = "Register a buy or sell transaction",
            description = "Sells cannot exceed the currently held quantity; the asset must exist and be active")
    @ApiResponse(responseCode = "404", description = "Portfolio or asset not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "422", description = "Business rule violation (e.g. selling more than available, inactive asset)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<TransactionResponse> create(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID portfolioId,
            @Valid @RequestBody TransactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.create(principal.getId(), portfolioId, request));
    }
}
