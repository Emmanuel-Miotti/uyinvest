package com.uyinvest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.uyinvest.dto.request.LoginRequest;
import com.uyinvest.dto.request.RegisterRequest;
import com.uyinvest.dto.response.AuthResponse;
import com.uyinvest.dto.response.UserResponse;
import com.uyinvest.entity.User;
import com.uyinvest.entity.enums.Role;
import com.uyinvest.exception.EmailAlreadyInUseException;
import com.uyinvest.mapper.UserMapper;
import com.uyinvest.repository.UserRepository;
import com.uyinvest.security.JwtTokenProvider;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AuthService authService;

    @Test
    void registersNewUserAndReturnsToken() {
        RegisterRequest request = new RegisterRequest("Emmanuel", "emmanuel@example.com", "password123");
        User savedUser = User.builder()
                .id(UUID.randomUUID())
                .name(request.name())
                .email(request.email())
                .password("encoded-password")
                .role(Role.USER)
                .build();

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("encoded-password");
        when(jwtTokenProvider.generateToken(any(), any())).thenReturn("jwt-token");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(86400000L);
        when(userMapper.toResponse(any(User.class)))
                .thenReturn(new UserResponse(savedUser.getId(), savedUser.getName(), savedUser.getEmail(), Role.USER, null));

        AuthResponse response = authService.register(request);

        assertThat(response.accessToken()).isEqualTo("jwt-token");
        assertThat(response.user().email()).isEqualTo("emmanuel@example.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void rejectsRegistrationWithDuplicateEmail() {
        RegisterRequest request = new RegisterRequest("Emmanuel", "duplicate@example.com", "password123");
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyInUseException.class);
    }

    @Test
    void loginWithInvalidCredentialsPropagatesException() {
        LoginRequest request = new LoginRequest("emmanuel@example.com", "wrong-password");
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void loginWithValidCredentialsReturnsToken() {
        LoginRequest request = new LoginRequest("emmanuel@example.com", "password123");
        User user = User.builder()
                .id(UUID.randomUUID())
                .name("Emmanuel")
                .email(request.email())
                .password("encoded-password")
                .role(Role.USER)
                .build();

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateToken(any(), any())).thenReturn("jwt-token");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(86400000L);
        when(userMapper.toResponse(any(User.class)))
                .thenReturn(new UserResponse(user.getId(), user.getName(), user.getEmail(), Role.USER, null));

        AuthResponse response = authService.login(request);

        assertThat(response.accessToken()).isEqualTo("jwt-token");
        assertThat(response.user().email()).isEqualTo(request.email());
    }
}
