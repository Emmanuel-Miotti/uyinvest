package com.uyinvest.mapper;

import com.uyinvest.dto.response.AssetResponse;
import com.uyinvest.entity.Asset;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AssetMapper {

    AssetResponse toResponse(Asset asset);
}
