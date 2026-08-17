package com.uyinvest.controller;

import com.uyinvest.dto.request.PortfolioRequest;
import com.uyinvest.dto.response.AllocationResponse;
import com.uyinvest.dto.response.PerformancePointResponse;
import com.uyinvest.dto.response.PortfolioResponse;
import com.uyinvest.dto.response.PortfolioSummaryResponse;
import com.uyinvest.security.CustomUserDetails;
import com.uyinvest.service.PortfolioDashboardService;
import com.uyinvest.service.PortfolioService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/portfolios")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final PortfolioDashboardService portfolioDashboardService;

    @GetMapping
    public ResponseEntity<List<PortfolioResponse>> getAll(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(portfolioService.getAllForUser(principal.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PortfolioResponse> getById(
            @AuthenticationPrincipal CustomUserDetails principal, @PathVariable UUID id) {
        return ResponseEntity.ok(portfolioService.getById(principal.getId(), id));
    }

    @PostMapping
    public ResponseEntity<PortfolioResponse> create(
            @AuthenticationPrincipal CustomUserDetails principal, @Valid @RequestBody PortfolioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(portfolioService.create(principal.getUser(), request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PortfolioResponse> update(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable UUID id,
            @Valid @RequestBody PortfolioRequest request) {
        return ResponseEntity.ok(portfolioService.update(principal.getId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal CustomUserDetails principal, @PathVariable UUID id) {
        portfolioService.delete(principal.getId(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/summary")
    public ResponseEntity<PortfolioSummaryResponse> getSummary(
            @AuthenticationPrincipal CustomUserDetails principal, @PathVariable UUID id) {
        return ResponseEntity.ok(portfolioDashboardService.getSummary(principal.getId(), id));
    }

    @GetMapping("/{id}/allocation")
    public ResponseEntity<List<AllocationResponse>> getAllocation(
            @AuthenticationPrincipal CustomUserDetails principal, @PathVariable UUID id) {
        return ResponseEntity.ok(portfolioDashboardService.getAllocation(principal.getId(), id));
    }

    @GetMapping("/{id}/performance")
    public ResponseEntity<List<PerformancePointResponse>> getPerformance(
            @AuthenticationPrincipal CustomUserDetails principal, @PathVariable UUID id) {
        return ResponseEntity.ok(portfolioDashboardService.getPerformance(principal.getId(), id));
    }
}
