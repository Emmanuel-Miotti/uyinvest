package com.uyinvest.mapper;

import com.uyinvest.dto.response.PortfolioResponse;
import com.uyinvest.entity.Portfolio;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PortfolioMapper {

    PortfolioResponse toResponse(Portfolio portfolio);
}
