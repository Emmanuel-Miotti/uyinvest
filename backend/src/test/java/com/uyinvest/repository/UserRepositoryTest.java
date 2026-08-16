package com.uyinvest.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.uyinvest.entity.User;
import com.uyinvest.entity.enums.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

class UserRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void savesAndFindsUserByEmail() {
        User user = User.builder()
                .name("Emmanuel")
                .email("emmanuel@example.com")
                .password("hashed-password")
                .role(Role.USER)
                .build();

        userRepository.save(user);

        assertThat(userRepository.findByEmail("emmanuel@example.com"))
                .isPresent()
                .get()
                .satisfies(found -> {
                    assertThat(found.getId()).isNotNull();
                    assertThat(found.getCreatedAt()).isNotNull();
                    assertThat(found.getUpdatedAt()).isNotNull();
                    assertThat(found.getRole()).isEqualTo(Role.USER);
                });
    }

    @Test
    void rejectsDuplicateEmail() {
        userRepository.saveAndFlush(User.builder()
                .name("Emmanuel")
                .email("duplicate@example.com")
                .password("hashed-password")
                .role(Role.USER)
                .build());

        User duplicate = User.builder()
                .name("Another User")
                .email("duplicate@example.com")
                .password("hashed-password")
                .role(Role.USER)
                .build();

        assertThatThrownBy(() -> userRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
