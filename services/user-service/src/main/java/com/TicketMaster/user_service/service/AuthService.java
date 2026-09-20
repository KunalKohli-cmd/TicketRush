package com.TicketMaster.user_service.service;

import com.TicketMaster.user_service.dto.AuthResponse;
import com.TicketMaster.user_service.dto.LoginRequest;
import com.TicketMaster.user_service.dto.SignupRequest;
import com.TicketMaster.user_service.entity.Role;
import com.TicketMaster.user_service.entity.User;
import com.TicketMaster.user_service.exceptions.EmailAlreadyExistsException;
import com.TicketMaster.user_service.exceptions.InvalidCredentialsException;
import com.TicketMaster.user_service.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final LoginAttemptService loginAttemptService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       RefreshTokenService refreshTokenService,
                       LoginAttemptService loginAttemptService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.loginAttemptService = loginAttemptService;
    }

    public AuthResponse signup(SignupRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new EmailAlreadyExistsException(req.email());
        }
        User user = new User();
        user.setName(req.name());
        user.setEmail(req.email());
        user.setPassword(passwordEncoder.encode(req.password()));
        user.setRole(Role.USER);
        userRepository.save(user);

        String accessToken = jwtService.createAccessToken(user);
        String refreshToken = refreshTokenService.create(user.getId());
        return new AuthResponse(accessToken, refreshToken);
    }

    public AuthResponse login(LoginRequest req) {
        if (loginAttemptService.isBlocked(req.email())) {
            throw new InvalidCredentialsException();
        }

        User user = userRepository.findByEmail(req.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(req.password(), user.getPassword())) {
            loginAttemptService.recordFailure(req.email());
            throw new InvalidCredentialsException();
        }

        loginAttemptService.resetAttempts(req.email());

        String accessToken = jwtService.createAccessToken(user);
        String refreshToken = refreshTokenService.create(user.getId());
        return new AuthResponse(accessToken, refreshToken);
    }

    public AuthResponse refresh(String refreshToken) {
        Long userId = refreshTokenService.validate(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(InvalidCredentialsException::new);

        refreshTokenService.revoke(refreshToken);

        String newAccessToken = jwtService.createAccessToken(user);
        String newRefreshToken = refreshTokenService.create(user.getId());
        return new AuthResponse(newAccessToken, newRefreshToken);
    }

    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }
}
