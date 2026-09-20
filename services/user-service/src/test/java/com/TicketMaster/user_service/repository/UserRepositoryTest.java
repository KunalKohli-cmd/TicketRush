package com.TicketMaster.user_service.repository;

import com.TicketMaster.user_service.entity.Role;
import com.TicketMaster.user_service.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryTest {

    @Autowired TestEntityManager entityManager;
    @Autowired UserRepository userRepository;

    @Test
    void findByEmail_existingUser_returnsUser() {
        User user = new User();
        user.setEmail("alice@example.com");
        user.setPassword("hashed");
        user.setName("Alice");
        user.setRole(Role.USER);
        entityManager.persistAndFlush(user);

        Optional<User> found = userRepository.findByEmail("alice@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("alice@example.com");
        assertThat(found.get().getName()).isEqualTo("Alice");
    }

    @Test
    void findByEmail_unknownEmail_returnsEmpty() {
        Optional<User> found = userRepository.findByEmail("unknown@example.com");
        assertThat(found).isEmpty();
    }

    @Test
    void existsByEmail_existingUser_returnsTrue() {
        User user = new User();
        user.setEmail("bob@example.com");
        user.setPassword("hashed");
        user.setName("Bob");
        user.setRole(Role.USER);
        entityManager.persistAndFlush(user);

        assertThat(userRepository.existsByEmail("bob@example.com")).isTrue();
    }

    @Test
    void existsByEmail_unknownEmail_returnsFalse() {
        assertThat(userRepository.existsByEmail("nobody@example.com")).isFalse();
    }
}
