package com.example.zadanie.repository;

import com.example.zadanie.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSaveAndFindUser() {
        User user = new User(null, "John Doe", "john@example.com", "password123");
        User saved = userRepository.save(user);

        assertThat(saved.getId()).isNotNull();
        assertThat(userRepository.findById(saved.getId())).isPresent();
    }

    @Test
    void shouldFindUserByEmail() {
        User user = new User(null, "Jane Doe", "jane@example.com", "password123");
        userRepository.save(user);

        assertThat(userRepository.findByEmail("jane@example.com")).isPresent();
    }

    @Test
    void shouldCheckEmailExists() {
        User user = new User(null, "Test User", "test@example.com", "password123");
        userRepository.save(user);

        assertThat(userRepository.existsByEmail("test@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("nonexistent@example.com")).isFalse();
    }
}
