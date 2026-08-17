package com.uyinvest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.uyinvest.dto.request.AssetRequest;
import com.uyinvest.dto.response.AssetResponse;
import com.uyinvest.entity.Asset;
import com.uyinvest.entity.enums.AssetType;
import com.uyinvest.exception.DuplicateResourceException;
import com.uyinvest.exception.ResourceNotFoundException;
import com.uyinvest.mapper.AssetMapper;
import com.uyinvest.repository.AssetRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AssetServiceTest {

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private AssetMapper assetMapper;

    @InjectMocks
    private AssetService assetService;

    private Asset asset(UUID id, String symbol) {
        return Asset.builder()
                .id(id)
                .symbol(symbol)
                .name(symbol + " Inc.")
                .type(AssetType.STOCK)
                .currency("USD")
                .active(true)
                .build();
    }

    @Test
    void createsAssetWithUppercasedSymbol() {
        AssetRequest request = new AssetRequest("aapl", "Apple Inc.", AssetType.STOCK, "USD", "Technology", true);

        when(assetRepository.existsBySymbol("AAPL")).thenReturn(false);
        when(assetRepository.save(any(Asset.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(assetMapper.toResponse(any(Asset.class)))
                .thenAnswer(invocation -> {
                    Asset a = invocation.getArgument(0);
                    return new AssetResponse(UUID.randomUUID(), a.getSymbol(), a.getName(), a.getType(), a.getCurrency(), a.getSector(), a.isActive(), null, null);
                });

        AssetResponse response = assetService.create(request);

        assertThat(response.symbol()).isEqualTo("AAPL");
        verify(assetRepository).save(any(Asset.class));
    }

    @Test
    void rejectsCreateWithDuplicateSymbol() {
        AssetRequest request = new AssetRequest("AAPL", "Apple Inc.", AssetType.STOCK, "USD", "Technology", true);
        when(assetRepository.existsBySymbol("AAPL")).thenReturn(true);

        assertThatThrownBy(() -> assetService.create(request))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void getByIdThrowsNotFoundWhenMissing() {
        UUID id = UUID.randomUUID();
        when(assetRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> assetService.getById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateAllowsKeepingTheSameSymbol() {
        UUID id = UUID.randomUUID();
        Asset existing = asset(id, "AAPL");
        AssetRequest request = new AssetRequest("aapl", "Apple Inc. Updated", AssetType.STOCK, "USD", "Tech", false);

        when(assetRepository.findById(id)).thenReturn(Optional.of(existing));
        when(assetMapper.toResponse(existing))
                .thenReturn(new AssetResponse(id, "AAPL", "Apple Inc. Updated", AssetType.STOCK, "USD", "Tech", false, null, null));

        AssetResponse response = assetService.update(id, request);

        assertThat(response.name()).isEqualTo("Apple Inc. Updated");
        assertThat(response.active()).isFalse();
    }

    @Test
    void rejectsUpdateWhenNewSymbolBelongsToAnotherAsset() {
        UUID id = UUID.randomUUID();
        Asset existing = asset(id, "AAPL");
        AssetRequest request = new AssetRequest("MSFT", "Renamed", AssetType.STOCK, "USD", "Tech", true);

        when(assetRepository.findById(id)).thenReturn(Optional.of(existing));
        when(assetRepository.existsBySymbol("MSFT")).thenReturn(true);

        assertThatThrownBy(() -> assetService.update(id, request))
                .isInstanceOf(DuplicateResourceException.class);
    }
}
