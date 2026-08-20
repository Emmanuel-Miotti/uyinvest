package com.uyinvest.repository;

import com.uyinvest.entity.Dividend;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DividendRepository extends JpaRepository<Dividend, UUID>, JpaSpecificationExecutor<Dividend> {

    List<Dividend> findByPortfolioId(UUID portfolioId);

    List<Dividend> findByPortfolioIdAndAssetId(UUID portfolioId, UUID assetId);

    @Override
    @EntityGraph(attributePaths = "asset")
    List<Dividend> findAll(Specification<Dividend> spec, Sort sort);
}
