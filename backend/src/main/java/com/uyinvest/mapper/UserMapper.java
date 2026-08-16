package com.uyinvest.mapper;

import com.uyinvest.dto.response.UserResponse;
import com.uyinvest.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(User user);
}
