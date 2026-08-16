package com.uyinvest.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.uyinvest.AbstractIntegrationTest;
import com.uyinvest.entity.Asset;
import com.uyinvest.entity.Dividend;
import com.uyinvest.entity.Portfolio;
import com.uyinvest.entity.User;
import com.uyinvest.entity.enums.AssetType;
import com.uyinvest.entity.enums.Role;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

class DividendRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private DividendRepository dividendRepository;

    private Portfolio createPortfolio(String email) {
        User user = userRepository.save(User.builder()
                .name("Emmanuel")
                .email(email)
                .password("hashed-password")
                .role(Role.USER)
                .build());

        return portfolioRepository.save(Portfolio.builder()
                .name("Cartera")
                .user(user)
                .baseCurrency("USD")
                .build());
    }

    private Asset createAsset(String symbol) {
        return assetRepository.save(Asset.builder()
                .symbol(symbol)
                .name(symbol + " Inc.")
                .type(AssetType.STOCK)
                .currency("USD")
                .active(true)
                .build());
    }

    @Test
    void savesAndFindsDividendByPortfolio() {
        Portfolio portfolio = createPortfolio("dividend1@example.com");
        Asset asset = createAsset("KO");

        Dividend dividend = dividendRepository.save(Dividend.builder()
                .portfolio(portfolio)
                .asset(asset)
                .amount(new BigDecimal("25.50"))
                .currency("USD")
                .paymentDate(LocalDate.of(2026, 6, 1))
                .build());

        List<Dividend> found = dividendRepository.findByPortfolioId(portfolio.getId());

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getId()).isEqualTo(dividend.getId());
    }

    @Test
    void rejectsNonPositiveAmount() {
        Portfolio portfolio = createPortfolio("dividend2@example.com");
        Asset asset = createAsset("PEP");

        Dividend invalid = Dividend.builder()
                .portfolio(portfolio)
                .asset(asset)
                .amount(BigDecimal.ZERO)
                .currency("USD")
                .paymentDate(LocalDate.of(2026, 6, 1))
                .build();

        assertThatThrownBy(() -> dividendRepository.saveAndFlush(invalid))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
