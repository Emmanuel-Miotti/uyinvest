package com.uyinvest.service;

import com.uyinvest.dto.request.DividendRequest;
import com.uyinvest.dto.response.DividendResponse;
import com.uyinvest.dto.response.DividendSummaryResponse;
import com.uyinvest.entity.Asset;
import com.uyinvest.entity.Dividend;
import com.uyinvest.entity.Portfolio;
import com.uyinvest.exception.ResourceNotFoundException;
import com.uyinvest.mapper.DividendMapper;
import com.uyinvest.repository.AssetRepository;
import com.uyinvest.repository.DividendRepository;
import com.uyinvest.repository.DividendSpecifications;
import com.uyinvest.repository.PortfolioRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DividendService {

    private final DividendRepository dividendRepository;
    private final PortfolioRepository portfolioRepository;
    private final AssetRepository assetRepository;
    private final DividendMapper dividendMapper;

    public List<DividendResponse> getAllForPortfolio(UUID userId, UUID portfolioId, UUID assetId, LocalDate from, LocalDate to) {
        findOwnedPortfolio(userId, portfolioId);

        Specification<Dividend> spec = Specification.allOf(
                DividendSpecifications.belongsToPortfolio(portfolioId),
                DividendSpecifications.hasAsset(assetId),
                DividendSpecifications.paidBetween(from, to));

        return dividendRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "paymentDate")).stream()
                .map(dividendMapper::toResponse)
                .toList();
    }

    @Transactional
    public DividendResponse create(UUID userId, UUID portfolioId, DividendRequest request) {
        Portfolio portfolio = findOwnedPortfolio(userId, portfolioId);

        Asset asset = assetRepository.findById(request.assetId())
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found: " + request.assetId()));

        Dividend dividend = Dividend.builder()
                .portfolio(portfolio)
                .asset(asset)
                .amount(request.amount())
                .currency(request.currency())
                .paymentDate(request.paymentDate())
                .build();

        return dividendMapper.toResponse(dividendRepository.save(dividend));
    }

    public DividendSummaryResponse getSummary(UUID userId, UUID portfolioId) {
        findOwnedPortfolio(userId, portfolioId);
        List<Dividend> dividends = dividendRepository.findByPortfolioId(portfolioId);

        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.from(today);

        BigDecimal totalThisMonth = sumWhere(dividends, d -> YearMonth.from(d.getPaymentDate()).equals(currentMonth));
        BigDecimal totalThisYear = sumWhere(dividends, d -> d.getPaymentDate().getYear() == today.getYear());
        BigDecimal totalHistorical = sumWhere(dividends, d -> true);

        return new DividendSummaryResponse(totalThisMonth, totalThisYear, totalHistorical);
    }

    private BigDecimal sumWhere(List<Dividend> dividends, Predicate<Dividend> predicate) {
        return dividends.stream()
                .filter(predicate)
                .map(Dividend::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Portfolio findOwnedPortfolio(UUID userId, UUID portfolioId) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found: " + portfolioId));

        if (!portfolio.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Portfolio not found: " + portfolioId);
        }

        return portfolio;
    }
}
