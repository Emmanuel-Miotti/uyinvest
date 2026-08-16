package com.uyinvest.repository;

import com.uyinvest.entity.Dividend;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DividendRepository extends JpaRepository<Dividend, UUID> {

    List<Dividend> findByPortfolioId(UUID portfolioId);

    List<Dividend> findByPortfolioIdAndAssetId(UUID portfolioId, UUID assetId);
}
