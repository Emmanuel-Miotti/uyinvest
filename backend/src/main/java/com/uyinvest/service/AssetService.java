package com.uyinvest.service;

import com.uyinvest.dto.request.AssetRequest;
import com.uyinvest.dto.response.AssetResponse;
import com.uyinvest.entity.Asset;
import com.uyinvest.entity.enums.AssetType;
import com.uyinvest.exception.DuplicateResourceException;
import com.uyinvest.exception.ResourceNotFoundException;
import com.uyinvest.mapper.AssetMapper;
import com.uyinvest.repository.AssetRepository;
import com.uyinvest.repository.AssetSpecifications;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssetService {

    private final AssetRepository assetRepository;
    private final AssetMapper assetMapper;

    public Page<AssetResponse> search(AssetType type, String search, Pageable pageable) {
        Specification<Asset> spec = Specification.allOf(
                AssetSpecifications.hasType(type),
                AssetSpecifications.matchesSearch(search));

        return assetRepository.findAll(spec, pageable).map(assetMapper::toResponse);
    }

    public AssetResponse getById(UUID id) {
        return assetMapper.toResponse(findAsset(id));
    }

    @Transactional
    public AssetResponse create(AssetRequest request) {
        String symbol = request.symbol().toUpperCase();
        if (assetRepository.existsBySymbol(symbol)) {
            throw new DuplicateResourceException("Asset symbol already exists: " + symbol);
        }

        Asset asset = Asset.builder()
                .symbol(symbol)
                .name(request.name())
                .type(request.type())
                .currency(request.currency())
                .sector(request.sector())
                .active(request.active())
                .build();

        return assetMapper.toResponse(assetRepository.save(asset));
    }

    @Transactional
    public AssetResponse update(UUID id, AssetRequest request) {
        Asset asset = findAsset(id);
        String symbol = request.symbol().toUpperCase();

        if (!symbol.equals(asset.getSymbol()) && assetRepository.existsBySymbol(symbol)) {
            throw new DuplicateResourceException("Asset symbol already exists: " + symbol);
        }

        asset.setSymbol(symbol);
        asset.setName(request.name());
        asset.setType(request.type());
        asset.setCurrency(request.currency());
        asset.setSector(request.sector());
        asset.setActive(request.active());

        return assetMapper.toResponse(asset);
    }

    private Asset findAsset(UUID id) {
        return assetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found: " + id));
    }
}
