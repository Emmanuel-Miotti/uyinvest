package com.uyinvest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.uyinvest.dto.request.PortfolioRequest;
import com.uyinvest.dto.response.PortfolioResponse;
import com.uyinvest.entity.Portfolio;
import com.uyinvest.entity.User;
import com.uyinvest.entity.enums.Role;
import com.uyinvest.exception.ResourceNotFoundException;
import com.uyinvest.mapper.PortfolioMapper;
import com.uyinvest.repository.PortfolioRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private PortfolioMapper portfolioMapper;

    @InjectMocks
    private PortfolioService portfolioService;

    private User owner(UUID id) {
        return User.builder().id(id).name("Owner").email("owner@example.com").role(Role.USER).build();
    }

    private Portfolio portfolioOf(UUID ownerId) {
        return Portfolio.builder()
                .id(UUID.randomUUID())
                .name("Cartera")
                .baseCurrency("USD")
                .user(owner(ownerId))
                .build();
    }

    @Test
    void createsPortfolioForOwner() {
        UUID ownerId = UUID.randomUUID();
        User owner = owner(ownerId);
        PortfolioRequest request = new PortfolioRequest("Cartera", "desc", "USD");

        when(portfolioRepository.save(any(Portfolio.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(portfolioMapper.toResponse(any(Portfolio.class)))
                .thenReturn(new PortfolioResponse(UUID.randomUUID(), "Cartera", "desc", "USD", null, null));

        PortfolioResponse response = portfolioService.create(owner, request);

        assertThat(response.name()).isEqualTo("Cartera");
        verify(portfolioRepository).save(any(Portfolio.class));
    }

    @Test
    void getByIdReturnsPortfolioWhenOwnedByUser() {
        UUID ownerId = UUID.randomUUID();
        Portfolio portfolio = portfolioOf(ownerId);

        when(portfolioRepository.findById(portfolio.getId())).thenReturn(Optional.of(portfolio));
        when(portfolioMapper.toResponse(portfolio))
                .thenReturn(new PortfolioResponse(portfolio.getId(), "Cartera", null, "USD", null, null));

        PortfolioResponse response = portfolioService.getById(ownerId, portfolio.getId());

        assertThat(response.id()).isEqualTo(portfolio.getId());
    }

    @Test
    void getByIdThrowsNotFoundWhenPortfolioBelongsToAnotherUser() {
        UUID ownerId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        Portfolio portfolio = portfolioOf(ownerId);

        when(portfolioRepository.findById(portfolio.getId())).thenReturn(Optional.of(portfolio));

        assertThatThrownBy(() -> portfolioService.getById(otherUserId, portfolio.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getByIdThrowsNotFoundWhenPortfolioDoesNotExist() {
        UUID userId = UUID.randomUUID();
        UUID portfolioId = UUID.randomUUID();

        when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> portfolioService.getById(userId, portfolioId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteThrowsNotFoundWhenPortfolioBelongsToAnotherUser() {
        UUID ownerId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        Portfolio portfolio = portfolioOf(ownerId);

        when(portfolioRepository.findById(portfolio.getId())).thenReturn(Optional.of(portfolio));

        assertThatThrownBy(() -> portfolioService.delete(otherUserId, portfolio.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
