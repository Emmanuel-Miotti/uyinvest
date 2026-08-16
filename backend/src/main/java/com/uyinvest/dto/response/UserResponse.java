package com.uyinvest.dto.response;

import com.uyinvest.entity.enums.Role;
import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String name,
        String email,
        Role role,
        Instant createdAt) {
}
