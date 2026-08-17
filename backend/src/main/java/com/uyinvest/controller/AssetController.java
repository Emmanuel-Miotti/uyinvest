package com.uyinvest.controller;

import com.uyinvest.dto.request.AssetRequest;
import com.uyinvest.dto.response.AssetResponse;
import com.uyinvest.entity.enums.AssetType;
import com.uyinvest.service.AssetService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;

    @GetMapping
    public ResponseEntity<Page<AssetResponse>> search(
            @RequestParam(required = false) AssetType type,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "symbol") Pageable pageable) {
        return ResponseEntity.ok(assetService.search(type, search, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AssetResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(assetService.getById(id));
    }

    @PostMapping
    public ResponseEntity<AssetResponse> create(@Valid @RequestBody AssetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(assetService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AssetResponse> update(@PathVariable UUID id, @Valid @RequestBody AssetRequest request) {
        return ResponseEntity.ok(assetService.update(id, request));
    }
}
