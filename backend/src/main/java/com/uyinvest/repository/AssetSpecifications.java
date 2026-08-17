package com.uyinvest.repository;

import com.uyinvest.entity.Asset;
import com.uyinvest.entity.enums.AssetType;
import org.springframework.data.jpa.domain.Specification;

public final class AssetSpecifications {

    private AssetSpecifications() {
    }

    public static Specification<Asset> hasType(AssetType type) {
        return (root, query, cb) -> type == null ? null : cb.equal(root.get("type"), type);
    }

    public static Specification<Asset> matchesSearch(String search) {
        if (search == null || search.isBlank()) {
            return (root, query, cb) -> null;
        }
        String pattern = "%" + search.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("symbol")), pattern),
                cb.like(cb.lower(root.get("name")), pattern));
    }
}
