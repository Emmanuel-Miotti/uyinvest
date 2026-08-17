package com.uyinvest.controller;

import com.uyinvest.dto.request.DividendRequest;
import com.uyinvest.dto.response.DividendResponse;
import com.uyinvest.dto.response.DividendSummaryResponse;
import com.uyinvest.security.CustomUserDetails;
import com.uyinvest.service.DividendService;
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
public class DividendController {

    private final DividendService dividendService;

    @GetMapping
    public ResponseEntity<List<DividendResponse>> getAll(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID portfolioId,
            @RequestParam(required = false) UUID assetId,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        return ResponseEntity.ok(dividendService.getAllForPortfolio(principal.getId(), portfolioId, assetId, from, to));
    }

    @PostMapping
    public ResponseEntity<DividendResponse> create(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID portfolioId,
            @Valid @RequestBody DividendRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dividendService.create(principal.getId(), portfolioId, request));
    }

    @GetMapping("/summary")
    public ResponseEntity<DividendSummaryResponse> getSummary(
            @AuthenticationPrincipal CustomUserDetails principal, @PathVariable UUID portfolioId) {
        return ResponseEntity.ok(dividendService.getSummary(principal.getId(), portfolioId));
    }
}
