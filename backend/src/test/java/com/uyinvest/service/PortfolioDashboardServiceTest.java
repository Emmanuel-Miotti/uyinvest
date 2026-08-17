package com.uyinvest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.uyinvest.dto.response.AllocationResponse;
import com.uyinvest.dto.response.PerformancePointResponse;
import com.uyinvest.dto.response.PortfolioSummaryResponse;
import com.uyinvest.entity.Asset;
import com.uyinvest.entity.Portfolio;
import com.uyinvest.entity.Transaction;
import com.uyinvest.entity.User;
import com.uyinvest.entity.enums.AssetType;
import com.uyinvest.entity.enums.Role;
import com.uyinvest.entity.enums.TransactionType;
import com.uyinvest.exception.ResourceNotFoundException;
import com.uyinvest.repository.PortfolioRepository;
import com.uyinvest.repository.TransactionRepository;
import com.uyinvest.service.marketdata.MarketDataProvider;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PortfolioDashboardServiceTest {

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private MarketDataProvider marketDataProvider;

    private final PositionCalculator positionCalculator = new PositionCalculator();

    private PortfolioDashboardService dashboardService() {
        return new PortfolioDashboardService(portfolioRepository, transactionRepository, positionCalculator, marketDataProvider);
    }

    private final UUID userId = UUID.randomUUID();
    private final UUID portfolioId = UUID.randomUUID();

    private Portfolio portfolio() {
        User owner = User.builder().id(userId).name("Owner").email("owner@example.com").role(Role.USER).build();
        return Portfolio.builder().id(portfolioId).name("Cartera").baseCurrency("USD").user(owner).build();
    }

    private Asset asset(AssetType type, String symbol) {
        return Asset.builder().id(UUID.randomUUID()).symbol(symbol).name(symbol).type(type).currency("USD").active(true).build();
    }

    private Transaction buy(Asset asset, String quantity, String price, Instant date) {
        return Transaction.builder()
                .asset(asset)
                .type(TransactionType.BUY)
                .quantity(new BigDecimal(quantity))
                .price(new BigDecimal(price))
                .commission(BigDecimal.ZERO)
                .currency("USD")
                .transactionDate(date)
                .createdAt(date)
                .build();
    }

    @Test
    void summaryAggregatesAcrossAllHeldAssets() {
        Asset aapl = asset(AssetType.STOCK, "AAPL");
        Asset spy = asset(AssetType.ETF, "SPY");
        Instant now = Instant.now();

        when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio()));
        when(transactionRepository.findDistinctAssetsByPortfolioId(portfolioId)).thenReturn(List.of(aapl, spy));
        when(transactionRepository.findByPortfolioIdAndAssetId(portfolioId, aapl.getId()))
                .thenReturn(List.of(buy(aapl, "10", "100", now)));
        when(transactionRepository.findByPortfolioIdAndAssetId(portfolioId, spy.getId()))
                .thenReturn(List.of(buy(spy, "1", "380", now)));
        when(marketDataProvider.getCurrentPrice("AAPL")).thenReturn(new BigDecimal("150"));
        when(marketDataProvider.getCurrentPrice("SPY")).thenReturn(new BigDecimal("400"));

        PortfolioSummaryResponse summary = dashboardService().getSummary(userId, portfolioId);

        assertThat(summary.totalInvested()).isEqualByComparingTo("1380");
        assertThat(summary.currentValue()).isEqualByComparingTo("1900");
        assertThat(summary.profitLoss()).isEqualByComparingTo("520");
    }

    @Test
    void summaryThrowsWhenPortfolioBelongsToAnotherUser() {
        UUID otherUserId = UUID.randomUUID();
        when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio()));

        assertThatThrownBy(() -> dashboardService().getSummary(otherUserId, portfolioId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void allocationGroupsByAssetTypeWithPercentages() {
        Asset aapl = asset(AssetType.STOCK, "AAPL");
        Asset spy = asset(AssetType.ETF, "SPY");
        Instant now = Instant.now();

        when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio()));
        when(transactionRepository.findDistinctAssetsByPortfolioId(portfolioId)).thenReturn(List.of(aapl, spy));
        when(transactionRepository.findByPortfolioIdAndAssetId(portfolioId, aapl.getId()))
                .thenReturn(List.of(buy(aapl, "10", "100", now)));
        when(transactionRepository.findByPortfolioIdAndAssetId(portfolioId, spy.getId()))
                .thenReturn(List.of(buy(spy, "10", "100", now)));
        when(marketDataProvider.getCurrentPrice("AAPL")).thenReturn(new BigDecimal("100"));
        when(marketDataProvider.getCurrentPrice("SPY")).thenReturn(new BigDecimal("300"));

        List<AllocationResponse> allocation = dashboardService().getAllocation(userId, portfolioId);

        assertThat(allocation).hasSize(2);
        AllocationResponse etf = allocation.stream().filter(a -> a.assetType() == AssetType.ETF).findFirst().orElseThrow();
        assertThat(etf.currentValue()).isEqualByComparingTo("3000");
        assertThat(etf.percentage()).isEqualByComparingTo("75.00");
    }

    @Test
    void performanceBuildsCumulativeInvestedTimelineAcrossAssets() {
        Asset aapl = asset(AssetType.STOCK, "AAPL");
        Asset spy = asset(AssetType.ETF, "SPY");
        Instant day1 = Instant.now().minus(2, ChronoUnit.DAYS);
        Instant day2 = Instant.now().minus(1, ChronoUnit.DAYS);

        Transaction firstBuy = buy(aapl, "10", "100", day1);
        Transaction secondBuy = buy(spy, "5", "200", day2);

        when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio()));
        when(transactionRepository.findByPortfolioId(portfolioId)).thenReturn(List.of(secondBuy, firstBuy));

        List<PerformancePointResponse> performance = dashboardService().getPerformance(userId, portfolioId);

        assertThat(performance).hasSize(2);
        assertThat(performance.get(0).totalInvested()).isEqualByComparingTo("1000");
        assertThat(performance.get(1).totalInvested()).isEqualByComparingTo("2000");
    }
}
