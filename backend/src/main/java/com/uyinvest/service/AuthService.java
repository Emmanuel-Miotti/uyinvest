package com.uyinvest.service;

import com.uyinvest.dto.request.LoginRequest;
import com.uyinvest.dto.request.RegisterRequest;
import com.uyinvest.dto.response.AuthResponse;
import com.uyinvest.dto.response.UserResponse;
import com.uyinvest.entity.User;
import com.uyinvest.entity.enums.Role;
import com.uyinvest.exception.EmailAlreadyInUseException;
import com.uyinvest.mapper.UserMapper;
import com.uyinvest.repository.UserRepository;
import com.uyinvest.security.CustomUserDetails;
import com.uyinvest.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserMapper userMapper;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyInUseException(request.email());
        }

        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.USER)
                .build();

        userRepository.save(user);

        return buildAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + request.email()));

        return buildAuthResponse(user);
    }

    public UserResponse getCurrentUser(CustomUserDetails principal) {
        return userMapper.toResponse(principal.getUser());
    }

    private AuthResponse buildAuthResponse(User user) {
        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail());
        return AuthResponse.of(token, jwtTokenProvider.getExpirationMs(), userMapper.toResponse(user));
    }
}
