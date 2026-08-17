package com.uyinvest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.uyinvest.dto.request.GoalRequest;
import com.uyinvest.dto.response.GoalResponse;
import com.uyinvest.entity.Goal;
import com.uyinvest.entity.User;
import com.uyinvest.entity.enums.Role;
import com.uyinvest.exception.ResourceNotFoundException;
import com.uyinvest.repository.GoalRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GoalServiceTest {

    @Mock
    private GoalRepository goalRepository;

    @InjectMocks
    private GoalService goalService;

    private final UUID userId = UUID.randomUUID();

    private User owner() {
        return User.builder().id(userId).name("Owner").email("owner@example.com").role(Role.USER).build();
    }

    private Goal goal(UUID id, UUID ownerId, String target, String current) {
        return Goal.builder()
                .id(id)
                .user(User.builder().id(ownerId).name("Owner").email("owner@example.com").role(Role.USER).build())
                .name("Comprar auto")
                .targetAmount(new BigDecimal(target))
                .currentAmount(new BigDecimal(current))
                .currency("USD")
                .build();
    }

    @Test
    void matchesSpecExampleProgress() {
        UUID goalId = UUID.randomUUID();
        when(goalRepository.findById(goalId)).thenReturn(Optional.of(goal(goalId, userId, "20000", "12500")));

        GoalResponse response = goalService.getById(userId, goalId);

        assertThat(response.progressPercentage()).isEqualByComparingTo("62.50");
    }

    @Test
    void createDefaultsCurrentAmountToZeroWhenOmitted() {
        GoalRequest request = new GoalRequest("Fondo de emergencia", new BigDecimal("5000"), null, "USD", null);
        when(goalRepository.save(any(Goal.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GoalResponse response = goalService.create(owner(), request);

        assertThat(response.currentAmount()).isEqualByComparingTo("0");
        assertThat(response.progressPercentage()).isEqualByComparingTo("0.00");
    }

    @Test
    void progressIsCappedAtOneHundredPercent() {
        UUID goalId = UUID.randomUUID();
        when(goalRepository.findById(goalId)).thenReturn(Optional.of(goal(goalId, userId, "1000", "1500")));

        GoalResponse response = goalService.getById(userId, goalId);

        assertThat(response.progressPercentage()).isEqualByComparingTo("100.00");
    }

    @Test
    void getByIdThrowsWhenGoalBelongsToAnotherUser() {
        UUID goalId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        when(goalRepository.findById(goalId)).thenReturn(Optional.of(goal(goalId, otherUserId, "1000", "0")));

        assertThatThrownBy(() -> goalService.getById(userId, goalId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateModifiesGoalFields() {
        UUID goalId = UUID.randomUUID();
        Goal existing = goal(goalId, userId, "1000", "0");
        when(goalRepository.findById(goalId)).thenReturn(Optional.of(existing));

        GoalRequest request = new GoalRequest("Nuevo nombre", new BigDecimal("2000"), new BigDecimal("500"), "EUR", null);
        GoalResponse response = goalService.update(userId, goalId, request);

        assertThat(response.name()).isEqualTo("Nuevo nombre");
        assertThat(response.targetAmount()).isEqualByComparingTo("2000");
        assertThat(response.currency()).isEqualTo("EUR");
    }

    @Test
    void deleteThrowsWhenGoalBelongsToAnotherUser() {
        UUID goalId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        when(goalRepository.findById(goalId)).thenReturn(Optional.of(goal(goalId, otherUserId, "1000", "0")));

        assertThatThrownBy(() -> goalService.delete(userId, goalId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
