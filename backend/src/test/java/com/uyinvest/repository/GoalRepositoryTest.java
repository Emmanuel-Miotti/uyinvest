package com.uyinvest.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.uyinvest.entity.Goal;
import com.uyinvest.entity.User;
import com.uyinvest.entity.enums.Role;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

class GoalRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GoalRepository goalRepository;

    private User createUser(String email) {
        return userRepository.save(User.builder()
                .name("Emmanuel")
                .email(email)
                .password("hashed-password")
                .role(Role.USER)
                .build());
    }

    @Test
    void savesAndFindsGoalByUser() {
        User user = createUser("goal1@example.com");

        Goal goal = goalRepository.save(Goal.builder()
                .user(user)
                .name("Comprar auto")
                .targetAmount(new BigDecimal("20000.00"))
                .currentAmount(new BigDecimal("12500.00"))
                .currency("USD")
                .targetDate(LocalDate.of(2027, 1, 1))
                .build());

        List<Goal> found = goalRepository.findByUserId(user.getId());

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getId()).isEqualTo(goal.getId());
    }

    @Test
    void rejectsNonPositiveTargetAmount() {
        User user = createUser("goal2@example.com");

        Goal invalid = Goal.builder()
                .user(user)
                .name("Meta inválida")
                .targetAmount(BigDecimal.ZERO)
                .currentAmount(BigDecimal.ZERO)
                .currency("USD")
                .build();

        assertThatThrownBy(() -> goalRepository.saveAndFlush(invalid))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsNegativeCurrentAmount() {
        User user = createUser("goal3@example.com");

        Goal invalid = Goal.builder()
                .user(user)
                .name("Meta inválida")
                .targetAmount(new BigDecimal("1000.00"))
                .currentAmount(new BigDecimal("-1"))
                .currency("USD")
                .build();

        assertThatThrownBy(() -> goalRepository.saveAndFlush(invalid))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
