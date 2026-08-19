package com.uyinvest.controller;

import com.uyinvest.dto.request.AssetRequest;
import com.uyinvest.dto.response.AssetResponse;
import com.uyinvest.entity.enums.AssetType;
import com.uyinvest.exception.ErrorResponse;
import com.uyinvest.service.AssetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Assets", description = "Asset catalog; create/update are ADMIN-only")
public class AssetController {

    private final AssetService assetService;

    @GetMapping
    @Operation(summary = "Search assets", description = "Supports filtering by type, free-text search, paging and sorting")
    public ResponseEntity<Page<AssetResponse>> search(
            @Parameter(description = "Filter by asset type") @RequestParam(required = false) AssetType type,
            @Parameter(description = "Case-insensitive match on symbol or name") @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "symbol") Pageable pageable) {
        return ResponseEntity.ok(assetService.search(type, search, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an asset by id")
    @ApiResponse(responseCode = "404", description = "Asset not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<AssetResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(assetService.getById(id));
    }

    @PostMapping
    @Operation(summary = "Create an asset (ADMIN only)")
    @ApiResponse(responseCode = "403", description = "Caller is not an ADMIN",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Symbol already exists",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<AssetResponse> create(@Valid @RequestBody AssetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(assetService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an asset (ADMIN only)")
    @ApiResponse(responseCode = "403", description = "Caller is not an ADMIN",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Asset not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Symbol already belongs to another asset",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<AssetResponse> update(@PathVariable UUID id, @Valid @RequestBody AssetRequest request) {
        return ResponseEntity.ok(assetService.update(id, request));
    }
}
