package com.uyinvest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.uyinvest.dto.request.TransactionRequest;
import com.uyinvest.dto.response.AssetResponse;
import com.uyinvest.dto.response.TransactionResponse;
import com.uyinvest.entity.Asset;
import com.uyinvest.entity.Portfolio;
import com.uyinvest.entity.Transaction;
import com.uyinvest.entity.User;
import com.uyinvest.entity.enums.AssetType;
import com.uyinvest.entity.enums.Role;
import com.uyinvest.entity.enums.TransactionType;
import com.uyinvest.exception.BusinessRuleException;
import com.uyinvest.exception.ResourceNotFoundException;
import com.uyinvest.mapper.TransactionMapper;
import com.uyinvest.repository.AssetRepository;
import com.uyinvest.repository.PortfolioRepository;
import com.uyinvest.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private TransactionMapper transactionMapper;

    @InjectMocks
    private TransactionService transactionService;

    private final UUID userId = UUID.randomUUID();
    private final UUID portfolioId = UUID.randomUUID();
    private final UUID assetId = UUID.randomUUID();

    private Portfolio portfolio() {
        User owner = User.builder().id(userId).name("Owner").email("owner@example.com").role(Role.USER).build();
        return Portfolio.builder().id(portfolioId).name("Cartera").baseCurrency("USD").user(owner).build();
    }

    private Asset asset(boolean active) {
        return Asset.builder().id(assetId).symbol("AAPL").name("Apple").type(AssetType.STOCK).currency("USD").active(active).build();
    }

    private TransactionRequest buyRequest(BigDecimal quantity) {
        return new TransactionRequest(assetId, TransactionType.BUY, quantity, new BigDecimal("100"), BigDecimal.ZERO, "USD", Instant.now());
    }

    private TransactionRequest sellRequest(BigDecimal quantity) {
        return new TransactionRequest(assetId, TransactionType.SELL, quantity, new BigDecimal("100"), BigDecimal.ZERO, "USD", Instant.now());
    }

    @Test
    void createsBuyTransaction() {
        when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio()));
        when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset(true)));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionMapper.toResponse(any(Transaction.class)))
                .thenReturn(new TransactionResponse(UUID.randomUUID(), mockAssetResponse(), TransactionType.BUY,
                        BigDecimal.TEN, new BigDecimal("100"), BigDecimal.ZERO, "USD", Instant.now(), Instant.now()));

        TransactionResponse response = transactionService.create(userId, portfolioId, buyRequest(BigDecimal.TEN));

        assertThat(response.type()).isEqualTo(TransactionType.BUY);
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void rejectsSellExceedingAvailableQuantity() {
        when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio()));
        when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset(true)));
        when(transactionRepository.getNetQuantity(portfolioId, assetId)).thenReturn(new BigDecimal("5"));

        assertThatThrownBy(() -> transactionService.create(userId, portfolioId, sellRequest(BigDecimal.TEN)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void allowsSellUpToAvailableQuantity() {
        when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio()));
        when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset(true)));
        when(transactionRepository.getNetQuantity(portfolioId, assetId)).thenReturn(BigDecimal.TEN);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionMapper.toResponse(any(Transaction.class)))
                .thenReturn(new TransactionResponse(UUID.randomUUID(), mockAssetResponse(), TransactionType.SELL,
                        BigDecimal.TEN, new BigDecimal("100"), BigDecimal.ZERO, "USD", Instant.now(), Instant.now()));

        TransactionResponse response = transactionService.create(userId, portfolioId, sellRequest(BigDecimal.TEN));

        assertThat(response.type()).isEqualTo(TransactionType.SELL);
    }

    @Test
    void rejectsTransactionOnInactiveAsset() {
        when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio()));
        when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset(false)));

        assertThatThrownBy(() -> transactionService.create(userId, portfolioId, buyRequest(BigDecimal.TEN)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void rejectsTransactionOnNonExistentAsset() {
        when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio()));
        when(assetRepository.findById(assetId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.create(userId, portfolioId, buyRequest(BigDecimal.TEN)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void rejectsTransactionOnPortfolioBelongingToAnotherUser() {
        UUID otherUserId = UUID.randomUUID();
        when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio()));

        assertThatThrownBy(() -> transactionService.create(otherUserId, portfolioId, buyRequest(BigDecimal.TEN)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private AssetResponse mockAssetResponse() {
        return new AssetResponse(assetId, "AAPL", "Apple", AssetType.STOCK, "USD", null, true, Instant.now(), Instant.now());
    }
}
