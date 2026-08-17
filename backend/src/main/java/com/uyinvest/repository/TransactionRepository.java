package com.uyinvest.repository;

import com.uyinvest.entity.Asset;
import com.uyinvest.entity.Transaction;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    List<Transaction> findByPortfolioId(UUID portfolioId);

    List<Transaction> findByPortfolioIdOrderByTransactionDateDesc(UUID portfolioId);

    List<Transaction> findByPortfolioIdAndAssetId(UUID portfolioId, UUID assetId);

    @Query("SELECT DISTINCT t.asset FROM Transaction t WHERE t.portfolio.id = :portfolioId")
    List<Asset> findDistinctAssetsByPortfolioId(@Param("portfolioId") UUID portfolioId);

    @Query("""
            SELECT COALESCE(SUM(
                CASE t.type
                    WHEN com.uyinvest.entity.enums.TransactionType.BUY THEN t.quantity
                    WHEN com.uyinvest.entity.enums.TransactionType.SELL THEN -t.quantity
                END), 0)
            FROM Transaction t
            WHERE t.portfolio.id = :portfolioId AND t.asset.id = :assetId
            """)
    BigDecimal getNetQuantity(@Param("portfolioId") UUID portfolioId, @Param("assetId") UUID assetId);
}
