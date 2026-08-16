package com.uyinvest.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.uyinvest.AbstractIntegrationTest;
import com.uyinvest.entity.Portfolio;
import com.uyinvest.entity.User;
import com.uyinvest.entity.enums.Role;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class PortfolioRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void savesPortfolioLinkedToUserAndFindsByUserId() {
        User user = userRepository.save(User.builder()
                .name("Emmanuel")
                .email("owner@example.com")
                .password("hashed-password")
                .role(Role.USER)
                .build());

        Portfolio portfolio = portfolioRepository.save(Portfolio.builder()
                .name("Cartera principal")
                .description("Cartera de largo plazo")
                .user(user)
                .baseCurrency("USD")
                .build());

        List<Portfolio> found = portfolioRepository.findByUserId(user.getId());

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getId()).isEqualTo(portfolio.getId());
        assertThat(found.get(0).getBaseCurrency()).isEqualTo("USD");
    }

    @Test
    void deletingUserCascadesToPortfolios() {
        User user = userRepository.save(User.builder()
                .name("Emmanuel")
                .email("cascade@example.com")
                .password("hashed-password")
                .role(Role.USER)
                .build());

        portfolioRepository.saveAndFlush(Portfolio.builder()
                .name("Cartera temporal")
                .user(user)
                .baseCurrency("USD")
                .build());

        UUID userId = user.getId();

        // Avoids Hibernate's TransientObjectException on flush: it still holds the Portfolio->User link in memory.
        entityManager.clear();

        userRepository.deleteById(userId);
        userRepository.flush();

        assertThat(portfolioRepository.findByUserId(userId)).isEmpty();
    }
}
