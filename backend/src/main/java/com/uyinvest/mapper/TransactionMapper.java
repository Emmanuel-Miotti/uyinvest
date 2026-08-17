package com.uyinvest.mapper;

import com.uyinvest.dto.response.TransactionResponse;
import com.uyinvest.entity.Transaction;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = AssetMapper.class)
public interface TransactionMapper {

    TransactionResponse toResponse(Transaction transaction);
}
