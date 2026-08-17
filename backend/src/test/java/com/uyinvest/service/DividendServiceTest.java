package com.uyinvest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.uyinvest.dto.request.DividendRequest;
import com.uyinvest.dto.response.DividendResponse;
import com.uyinvest.entity.Asset;
import com.uyinvest.entity.Dividend;
import com.uyinvest.entity.Portfolio;
import com.uyinvest.entity.User;
import com.uyinvest.entity.enums.AssetType;
import com.uyinvest.entity.enums.Role;
import com.uyinvest.exception.ResourceNotFoundException;
import com.uyinvest.mapper.DividendMapper;
import com.uyinvest.repository.AssetRepository;
import com.uyinvest.repository.DividendRepository;
import com.uyinvest.repository.PortfolioRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DividendServiceTest {

    @Mock
    private DividendRepository dividendRepository;

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private DividendMapper dividendMapper;

    @InjectMocks
    private DividendService dividendService;

    private final UUID userId = UUID.randomUUID();
    private final UUID portfolioId = UUID.randomUUID();
    private final UUID assetId = UUID.randomUUID();

    private Portfolio portfolio() {
        User owner = User.builder().id(userId).name("Owner").email("owner@example.com").role(Role.USER).build();
        return Portfolio.builder().id(portfolioId).name("Cartera").baseCurrency("USD").user(owner).build();
    }

    private Asset asset() {
        return Asset.builder().id(assetId).symbol("KO").name("Coca-Cola").type(AssetType.STOCK).currency("USD").active(true).build();
    }

    private Dividend dividend(LocalDate paymentDate, String amount) {
        return Dividend.builder()
                .id(UUID.randomUUID())
                .portfolio(portfolio())
                .asset(asset())
                .amount(new BigDecimal(amount))
                .currency("USD")
                .paymentDate(paymentDate)
                .build();
    }

    @Test
    void createsDividendForOwnedPortfolio() {
        DividendRequest request = new DividendRequest(assetId, new BigDecimal("25.50"), "USD", LocalDate.now());

        when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio()));
        when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset()));
        when(dividendRepository.save(any(Dividend.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(dividendMapper.toResponse(any(Dividend.class)))
                .thenReturn(new DividendResponse(UUID.randomUUID(), null, new BigDecimal("25.50"), "USD", LocalDate.now(), null));

        DividendResponse response = dividendService.create(userId, portfolioId, request);

        assertThat(response.amount()).isEqualByComparingTo("25.50");
    }

    @Test
    void createThrowsWhenAssetDoesNotExist() {
        DividendRequest request = new DividendRequest(assetId, new BigDecimal("10"), "USD", LocalDate.now());

        when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio()));
        when(assetRepository.findById(assetId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dividendService.create(userId, portfolioId, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createThrowsWhenPortfolioBelongsToAnotherUser() {
        UUID otherUserId = UUID.randomUUID();
        DividendRequest request = new DividendRequest(assetId, new BigDecimal("10"), "USD", LocalDate.now());

        when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio()));

        assertThatThrownBy(() -> dividendService.create(otherUserId, portfolioId, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void summaryComputesMonthYearAndHistoricalTotalsSeparately() {
        LocalDate today = LocalDate.now();
        LocalDate lastYear = today.minusYears(1);

        when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio()));
        when(dividendRepository.findByPortfolioId(portfolioId)).thenReturn(List.of(
                dividend(today, "10"),
                dividend(today.withDayOfMonth(1), "20"),
                dividend(lastYear, "100")));

        var summary = dividendService.getSummary(userId, portfolioId);

        assertThat(summary.totalThisMonth()).isEqualByComparingTo("30");
        assertThat(summary.totalThisYear()).isEqualByComparingTo("30");
        assertThat(summary.totalHistorical()).isEqualByComparingTo("130");
    }
}
