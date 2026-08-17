package com.uyinvest.service;

import com.uyinvest.dto.request.GoalRequest;
import com.uyinvest.dto.response.GoalResponse;
import com.uyinvest.entity.Goal;
import com.uyinvest.entity.User;
import com.uyinvest.exception.ResourceNotFoundException;
import com.uyinvest.repository.GoalRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GoalService {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final GoalRepository goalRepository;

    public List<GoalResponse> getAllForUser(UUID userId) {
        return goalRepository.findByUserId(userId).stream().map(this::toResponse).toList();
    }

    public GoalResponse getById(UUID userId, UUID goalId) {
        return toResponse(findOwnedGoal(userId, goalId));
    }

    @Transactional
    public GoalResponse create(User owner, GoalRequest request) {
        Goal goal = Goal.builder()
                .user(owner)
                .name(request.name())
                .targetAmount(request.targetAmount())
                .currentAmount(request.currentAmount() != null ? request.currentAmount() : BigDecimal.ZERO)
                .currency(request.currency())
                .targetDate(request.targetDate())
                .build();

        return toResponse(goalRepository.save(goal));
    }

    @Transactional
    public GoalResponse update(UUID userId, UUID goalId, GoalRequest request) {
        Goal goal = findOwnedGoal(userId, goalId);

        goal.setName(request.name());
        goal.setTargetAmount(request.targetAmount());
        goal.setCurrentAmount(request.currentAmount() != null ? request.currentAmount() : BigDecimal.ZERO);
        goal.setCurrency(request.currency());
        goal.setTargetDate(request.targetDate());

        return toResponse(goal);
    }

    @Transactional
    public void delete(UUID userId, UUID goalId) {
        goalRepository.delete(findOwnedGoal(userId, goalId));
    }

    private Goal findOwnedGoal(UUID userId, UUID goalId) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new ResourceNotFoundException("Goal not found: " + goalId));

        if (!goal.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Goal not found: " + goalId);
        }

        return goal;
    }

    private GoalResponse toResponse(Goal goal) {
        BigDecimal progress = goal.getCurrentAmount()
                .divide(goal.getTargetAmount(), 4, RoundingMode.HALF_UP)
                .multiply(ONE_HUNDRED)
                .setScale(2, RoundingMode.HALF_UP)
                .min(ONE_HUNDRED);

        return new GoalResponse(
                goal.getId(),
                goal.getName(),
                goal.getTargetAmount(),
                goal.getCurrentAmount(),
                goal.getCurrency(),
                goal.getTargetDate(),
                progress,
                goal.getCreatedAt(),
                goal.getUpdatedAt());
    }
}
