package com.uyinvest.service;

import com.uyinvest.dto.response.AllocationResponse;
import com.uyinvest.dto.response.PerformancePointResponse;
import com.uyinvest.dto.response.PortfolioSummaryResponse;
import com.uyinvest.entity.Asset;
import com.uyinvest.entity.Portfolio;
import com.uyinvest.entity.Transaction;
import com.uyinvest.entity.enums.AssetType;
import com.uyinvest.exception.ResourceNotFoundException;
import com.uyinvest.repository.PortfolioRepository;
import com.uyinvest.repository.TransactionRepository;
import com.uyinvest.service.marketdata.MarketDataProvider;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortfolioDashboardService {

    private final PortfolioRepository portfolioRepository;
    private final TransactionRepository transactionRepository;
    private final PositionCalculator positionCalculator;
    private final MarketDataProvider marketDataProvider;

    public PortfolioSummaryResponse getSummary(UUID userId, UUID portfolioId) {
        assertOwnedByUser(userId, portfolioId);
        List<AssetPosition> positions = calculateOpenPositions(portfolioId);

        BigDecimal totalInvested = sum(positions, ap -> ap.position().costBasis());
        BigDecimal currentValue = sum(positions, ap -> ap.position().currentValue());
        BigDecimal profitLoss = currentValue.subtract(totalInvested);
        BigDecimal profitLossPercentage = percentageOf(profitLoss, totalInvested);

        return new PortfolioSummaryResponse(totalInvested, currentValue, profitLoss, profitLossPercentage);
    }

    public List<AllocationResponse> getAllocation(UUID userId, UUID portfolioId) {
        assertOwnedByUser(userId, portfolioId);
        List<AssetPosition> positions = calculateOpenPositions(portfolioId);
        BigDecimal totalValue = sum(positions, ap -> ap.position().currentValue());

        Map<AssetType, BigDecimal> valueByType = positions.stream()
                .collect(Collectors.groupingBy(
                        ap -> ap.asset().getType(),
                        LinkedHashMap::new,
                        Collectors.reducing(BigDecimal.ZERO, ap -> ap.position().currentValue(), BigDecimal::add)));

        return valueByType.entrySet().stream()
                .map(entry -> new AllocationResponse(entry.getKey(), entry.getValue(), percentageOf(entry.getValue(), totalValue)))
                .sorted(Comparator.comparing(AllocationResponse::currentValue).reversed())
                .toList();
    }

    public List<PerformancePointResponse> getPerformance(UUID userId, UUID portfolioId) {
        assertOwnedByUser(userId, portfolioId);

        List<Transaction> transactions = transactionRepository.findByPortfolioId(portfolioId).stream()
                .sorted(Comparator.comparing(Transaction::getTransactionDate).thenComparing(Transaction::getCreatedAt))
                .toList();

        Map<UUID, List<Transaction>> transactionsByAsset = new LinkedHashMap<>();
        List<PerformancePointResponse> points = new ArrayList<>();

        for (Transaction tx : transactions) {
            transactionsByAsset.computeIfAbsent(tx.getAsset().getId(), id -> new ArrayList<>()).add(tx);

            BigDecimal totalInvested = transactionsByAsset.values().stream()
                    .map(positionCalculator::calculateCostBasis)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            points.add(new PerformancePointResponse(tx.getTransactionDate(), totalInvested));
        }

        return points;
    }

    private List<AssetPosition> calculateOpenPositions(UUID portfolioId) {
        return transactionRepository.findDistinctAssetsByPortfolioId(portfolioId).stream()
                .map(asset -> {
                    List<Transaction> transactions = transactionRepository.findByPortfolioIdAndAssetId(portfolioId, asset.getId());
                    BigDecimal currentPrice = marketDataProvider.getCurrentPrice(asset.getSymbol());
                    return new AssetPosition(asset, positionCalculator.calculate(transactions, currentPrice));
                })
                .filter(ap -> ap.position().quantity().signum() > 0)
                .toList();
    }

    private BigDecimal sum(List<AssetPosition> positions, java.util.function.Function<AssetPosition, BigDecimal> extractor) {
        return positions.stream().map(extractor).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal percentageOf(BigDecimal amount, BigDecimal total) {
        return total.signum() > 0
                ? amount.divide(total, 6, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
    }

    private void assertOwnedByUser(UUID userId, UUID portfolioId) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found: " + portfolioId));

        if (!portfolio.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Portfolio not found: " + portfolioId);
        }
    }

    private record AssetPosition(Asset asset, Position position) {
    }
}
