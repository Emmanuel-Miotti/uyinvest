package com.uyinvest.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.uyinvest.AbstractIntegrationTest;
import com.uyinvest.entity.Asset;
import com.uyinvest.entity.Dividend;
import com.uyinvest.entity.Portfolio;
import com.uyinvest.entity.Transaction;
import com.uyinvest.entity.User;
import com.uyinvest.entity.enums.AssetType;
import com.uyinvest.entity.enums.Role;
import com.uyinvest.entity.enums.TransactionType;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;

class QueryCountTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private DividendRepository dividendRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private Portfolio createPortfolio(String email) {
        User user = userRepository.save(User.builder().name("Owner").email(email).password("x").role(Role.USER).build());
        return portfolioRepository.save(Portfolio.builder().name("Cartera").user(user).baseCurrency("USD").build());
    }

    private Asset createAsset(String symbol) {
        return assetRepository.save(Asset.builder().symbol(symbol).name(symbol).type(AssetType.STOCK).currency("USD").active(true).build());
    }

    private Statistics statistics() {
        Statistics stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        stats.setStatisticsEnabled(true);
        return stats;
    }

    @Test
    void listingTransactionsWithAssetsDoesNotTriggerNPlusOneQueries() {
        Portfolio portfolio = createPortfolio("npo-tx@example.com");

        for (int i = 0; i < 5; i++) {
            Asset asset = createAsset("SYM" + i);
            transactionRepository.save(Transaction.builder()
                    .portfolio(portfolio).asset(asset).type(TransactionType.BUY)
                    .quantity(BigDecimal.ONE).price(BigDecimal.TEN).commission(BigDecimal.ZERO)
                    .currency("USD").transactionDate(Instant.now()).build());
        }
        transactionRepository.flush();

        Statistics stats = statistics();
        stats.clear();

        var transactions = transactionRepository.findByPortfolioIdOrderByTransactionDateDesc(portfolio.getId());
        transactions.forEach(t -> t.getAsset().getSymbol());

        assertThat(transactions).hasSize(5);
        assertThat(stats.getPrepareStatementCount()).isLessThanOrEqualTo(2);
    }

    @Test
    void listingDividendsWithAssetsDoesNotTriggerNPlusOneQueries() {
        Portfolio portfolio = createPortfolio("npo-div@example.com");

        for (int i = 0; i < 5; i++) {
            Asset asset = createAsset("DIV" + i);
            dividendRepository.save(Dividend.builder()
                    .portfolio(portfolio).asset(asset).amount(BigDecimal.TEN)
                    .currency("USD").paymentDate(LocalDate.now()).build());
        }
        dividendRepository.flush();

        Statistics stats = statistics();
        stats.clear();

        var spec = DividendSpecifications.belongsToPortfolio(portfolio.getId());
        var dividends = dividendRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "paymentDate"));
        dividends.forEach(d -> d.getAsset().getSymbol());

        assertThat(dividends).hasSize(5);
        assertThat(stats.getPrepareStatementCount()).isLessThanOrEqualTo(2);
    }
}
