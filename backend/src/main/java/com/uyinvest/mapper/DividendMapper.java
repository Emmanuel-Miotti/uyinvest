package com.uyinvest.mapper;

import com.uyinvest.dto.response.DividendResponse;
import com.uyinvest.entity.Dividend;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = AssetMapper.class)
public interface DividendMapper {

    DividendResponse toResponse(Dividend dividend);
}
