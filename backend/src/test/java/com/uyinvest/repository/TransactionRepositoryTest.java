package com.uyinvest.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.uyinvest.entity.Asset;
import com.uyinvest.entity.Portfolio;
import com.uyinvest.entity.Transaction;
import com.uyinvest.entity.User;
import com.uyinvest.entity.enums.AssetType;
import com.uyinvest.entity.enums.Role;
import com.uyinvest.entity.enums.TransactionType;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

class TransactionRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private EntityManager entityManager;

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
    void savesBuyTransactionAndFindsByPortfolio() {
        Portfolio portfolio = createPortfolio("trader1@example.com");
        Asset asset = createAsset("MSFT");

        Transaction transaction = transactionRepository.save(Transaction.builder()
                .portfolio(portfolio)
                .asset(asset)
                .type(TransactionType.BUY)
                .quantity(new BigDecimal("10"))
                .price(new BigDecimal("100.00"))
                .commission(new BigDecimal("5.00"))
                .currency("USD")
                .transactionDate(Instant.now())
                .build());

        List<Transaction> found = transactionRepository.findByPortfolioId(portfolio.getId());

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getId()).isEqualTo(transaction.getId());
        assertThat(found.get(0).getQuantity()).isEqualByComparingTo("10");
    }

    @Test
    void rejectsNonPositiveQuantity() {
        Portfolio portfolio = createPortfolio("trader2@example.com");
        Asset asset = createAsset("GOOG");

        Transaction invalid = Transaction.builder()
                .portfolio(portfolio)
                .asset(asset)
                .type(TransactionType.BUY)
                .quantity(new BigDecimal("-1"))
                .price(new BigDecimal("100.00"))
                .commission(BigDecimal.ZERO)
                .currency("USD")
                .transactionDate(Instant.now())
                .build();

        assertThatThrownBy(() -> transactionRepository.saveAndFlush(invalid))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsNegativeCommission() {
        Portfolio portfolio = createPortfolio("trader3@example.com");
        Asset asset = createAsset("AMZN");

        Transaction invalid = Transaction.builder()
                .portfolio(portfolio)
                .asset(asset)
                .type(TransactionType.SELL)
                .quantity(new BigDecimal("5"))
                .price(new BigDecimal("50.00"))
                .commission(new BigDecimal("-1"))
                .currency("USD")
                .transactionDate(Instant.now())
                .build();

        assertThatThrownBy(() -> transactionRepository.saveAndFlush(invalid))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deletingPortfolioCascadesToTransactions() {
        Portfolio portfolio = createPortfolio("trader4@example.com");
        Asset asset = createAsset("TSLA");

        transactionRepository.saveAndFlush(Transaction.builder()
                .portfolio(portfolio)
                .asset(asset)
                .type(TransactionType.BUY)
                .quantity(new BigDecimal("2"))
                .price(new BigDecimal("200.00"))
                .commission(BigDecimal.ZERO)
                .currency("USD")
                .transactionDate(Instant.now())
                .build());

        UUID portfolioId = portfolio.getId();

        // Avoids Hibernate's TransientObjectException on flush: it still holds the Transaction->Portfolio link in memory.
        entityManager.clear();

        portfolioRepository.deleteById(portfolioId);
        portfolioRepository.flush();

        assertThat(transactionRepository.findByPortfolioId(portfolioId)).isEmpty();
    }
}
