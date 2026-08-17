package com.uyinvest.repository;

import com.uyinvest.entity.Dividend;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class DividendSpecifications {

    private DividendSpecifications() {
    }

    public static Specification<Dividend> belongsToPortfolio(UUID portfolioId) {
        return (root, query, cb) -> cb.equal(root.get("portfolio").get("id"), portfolioId);
    }

    public static Specification<Dividend> hasAsset(UUID assetId) {
        return (root, query, cb) -> assetId == null ? null : cb.equal(root.get("asset").get("id"), assetId);
    }

    public static Specification<Dividend> paidBetween(LocalDate from, LocalDate to) {
        return (root, query, cb) -> {
            if (from != null && to != null) {
                return cb.between(root.get("paymentDate"), from, to);
            }
            if (from != null) {
                return cb.greaterThanOrEqualTo(root.get("paymentDate"), from);
            }
            if (to != null) {
                return cb.lessThanOrEqualTo(root.get("paymentDate"), to);
            }
            return null;
        };
    }
}
