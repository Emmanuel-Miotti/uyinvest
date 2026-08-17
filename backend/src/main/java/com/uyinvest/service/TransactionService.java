package com.uyinvest.service;

import com.uyinvest.dto.request.TransactionRequest;
import com.uyinvest.dto.response.TransactionResponse;
import com.uyinvest.entity.Asset;
import com.uyinvest.entity.Portfolio;
import com.uyinvest.entity.Transaction;
import com.uyinvest.entity.enums.TransactionType;
import com.uyinvest.exception.BusinessRuleException;
import com.uyinvest.exception.ResourceNotFoundException;
import com.uyinvest.mapper.TransactionMapper;
import com.uyinvest.repository.AssetRepository;
import com.uyinvest.repository.PortfolioRepository;
import com.uyinvest.repository.TransactionRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final PortfolioRepository portfolioRepository;
    private final AssetRepository assetRepository;
    private final TransactionMapper transactionMapper;

    public List<TransactionResponse> getAllForPortfolio(UUID userId, UUID portfolioId) {
        findOwnedPortfolio(userId, portfolioId);
        return transactionRepository.findByPortfolioIdOrderByTransactionDateDesc(portfolioId).stream()
                .map(transactionMapper::toResponse)
                .toList();
    }

    @Transactional
    public TransactionResponse create(UUID userId, UUID portfolioId, TransactionRequest request) {
        Portfolio portfolio = findOwnedPortfolio(userId, portfolioId);

        Asset asset = assetRepository.findById(request.assetId())
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found: " + request.assetId()));

        if (!asset.isActive()) {
            throw new BusinessRuleException("Asset is not active and cannot be traded: " + asset.getSymbol());
        }

        if (request.type() == TransactionType.SELL) {
            BigDecimal available = transactionRepository.getNetQuantity(portfolioId, asset.getId());
            if (request.quantity().compareTo(available) > 0) {
                throw new BusinessRuleException(
                        "Cannot sell %s %s: only %s available".formatted(request.quantity(), asset.getSymbol(), available));
            }
        }

        Transaction transaction = Transaction.builder()
                .portfolio(portfolio)
                .asset(asset)
                .type(request.type())
                .quantity(request.quantity())
                .price(request.price())
                .commission(request.commission() != null ? request.commission() : BigDecimal.ZERO)
                .currency(request.currency())
                .transactionDate(request.transactionDate())
                .build();

        return transactionMapper.toResponse(transactionRepository.save(transaction));
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
