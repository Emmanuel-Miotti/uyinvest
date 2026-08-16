package com.uyinvest.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.uyinvest.entity.Asset;
import com.uyinvest.entity.enums.AssetType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

class AssetRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private AssetRepository assetRepository;

    @Test
    void savesAndFindsAssetBySymbol() {
        assetRepository.save(Asset.builder()
                .symbol("AAPL")
                .name("Apple Inc.")
                .type(AssetType.STOCK)
                .currency("USD")
                .sector("Technology")
                .active(true)
                .build());

        assertThat(assetRepository.findBySymbol("AAPL"))
                .isPresent()
                .get()
                .satisfies(asset -> assertThat(asset.getType()).isEqualTo(AssetType.STOCK));
    }

    @Test
    void rejectsDuplicateSymbol() {
        assetRepository.saveAndFlush(Asset.builder()
                .symbol("BTC")
                .name("Bitcoin")
                .type(AssetType.CRYPTO)
                .currency("USD")
                .active(true)
                .build());

        Asset duplicate = Asset.builder()
                .symbol("BTC")
                .name("Bitcoin duplicado")
                .type(AssetType.CRYPTO)
                .currency("USD")
                .active(true)
                .build();

        assertThatThrownBy(() -> assetRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
