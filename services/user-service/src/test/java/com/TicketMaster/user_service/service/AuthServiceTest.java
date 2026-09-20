package com.TicketMaster.user_service.service;

import com.TicketMaster.user_service.dto.AuthResponse;
import com.TicketMaster.user_service.dto.LoginRequest;
import com.TicketMaster.user_service.dto.SignupRequest;
import com.TicketMaster.user_service.entity.Role;
import com.TicketMaster.user_service.entity.User;
import com.TicketMaster.user_service.exceptions.EmailAlreadyExistsException;
import com.TicketMaster.user_service.exceptions.InvalidCredentialsException;
import com.TicketMaster.user_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock RefreshTokenService refreshTokenService;
    @Mock LoginAttemptService loginAttemptService;

    AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtService, refreshTokenService, loginAttemptService);
    }

    // ---------------------------------------------------------------
    // signup
    // ---------------------------------------------------------------

    @Test
    void signup_happyPath_returnsAuthResponse() {
        SignupRequest req = new SignupRequest("Alice", "alice@example.com", "password123");
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            return u;
        });
        when(jwtService.createAccessToken(any(User.class))).thenReturn("access-token");
        when(refreshTokenService.create(any())).thenReturn("refresh-token");

        AuthResponse response = authService.signup(req);

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertThat(saved.getEmail()).isEqualTo("alice@example.com");
        assertThat(saved.getName()).isEqualTo("Alice");
        assertThat(saved.getPassword()).isEqualTo("hashed");
        assertThat(saved.getRole()).isEqualTo(Role.USER);
    }

    @Test
    void signup_duplicateEmail_throwsEmailAlreadyExistsException() {
        SignupRequest req = new SignupRequest("Alice", "alice@example.com", "password123");
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.signup(req))
                .isInstanceOf(EmailAlreadyExistsException.class);
        verify(userRepository, never()).save(any());
    }

    // ---------------------------------------------------------------
    // login
    // ---------------------------------------------------------------

    @Test
    void login_happyPath_returnsAuthResponse() {
        LoginRequest req = new LoginRequest("alice@example.com", "password123");
        User user = buildUser(1L, "alice@example.com", "hashed", Role.USER);

        when(loginAttemptService.isBlocked("alice@example.com")).thenReturn(false);
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtService.createAccessToken(user)).thenReturn("access-token");
        when(refreshTokenService.create(1L)).thenReturn("refresh-token");

        AuthResponse response = authService.login(req);

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        verify(loginAttemptService).resetAttempts("alice@example.com");
    }

    @Test
    void login_wrongPassword_recordsFailureAndThrows() {
        LoginRequest req = new LoginRequest("alice@example.com", "wrongpassword");
        User user = buildUser(1L, "alice@example.com", "hashed", Role.USER);

        when(loginAttemptService.isBlocked("alice@example.com")).thenReturn(false);
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpassword", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(loginAttemptService).recordFailure("alice@example.com");
    }

    @Test
    void login_blockedAccount_throwsWithoutLookup() {
        LoginRequest req = new LoginRequest("alice@example.com", "password123");
        when(loginAttemptService.isBlocked("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(userRepository, never()).findByEmail(any());
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private User buildUser(Long id, String email, String password, Role role) {
        User u = new User();
        u.setEmail(email);
        u.setPassword(password);
        u.setName("Test User");
        u.setRole(role);
        return u;
    }
}
