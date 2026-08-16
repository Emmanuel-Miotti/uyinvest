package com.uyinvest.service;

import com.uyinvest.dto.request.PortfolioRequest;
import com.uyinvest.dto.response.PortfolioResponse;
import com.uyinvest.entity.Portfolio;
import com.uyinvest.entity.User;
import com.uyinvest.exception.ResourceNotFoundException;
import com.uyinvest.mapper.PortfolioMapper;
import com.uyinvest.repository.PortfolioRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final PortfolioMapper portfolioMapper;

    public List<PortfolioResponse> getAllForUser(UUID userId) {
        return portfolioRepository.findByUserId(userId).stream()
                .map(portfolioMapper::toResponse)
                .toList();
    }

    public PortfolioResponse getById(UUID userId, UUID portfolioId) {
        return portfolioMapper.toResponse(findOwnedPortfolio(userId, portfolioId));
    }

    @Transactional
    public PortfolioResponse create(User owner, PortfolioRequest request) {
        Portfolio portfolio = Portfolio.builder()
                .name(request.name())
                .description(request.description())
                .baseCurrency(request.baseCurrency())
                .user(owner)
                .build();

        return portfolioMapper.toResponse(portfolioRepository.save(portfolio));
    }

    @Transactional
    public PortfolioResponse update(UUID userId, UUID portfolioId, PortfolioRequest request) {
        Portfolio portfolio = findOwnedPortfolio(userId, portfolioId);

        portfolio.setName(request.name());
        portfolio.setDescription(request.description());
        portfolio.setBaseCurrency(request.baseCurrency());

        return portfolioMapper.toResponse(portfolio);
    }

    @Transactional
    public void delete(UUID userId, UUID portfolioId) {
        Portfolio portfolio = findOwnedPortfolio(userId, portfolioId);
        portfolioRepository.delete(portfolio);
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
