package com.uyinvest.repository;

import com.uyinvest.entity.Transaction;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    List<Transaction> findByPortfolioId(UUID portfolioId);

    List<Transaction> findByPortfolioIdAndAssetId(UUID portfolioId, UUID assetId);
}
