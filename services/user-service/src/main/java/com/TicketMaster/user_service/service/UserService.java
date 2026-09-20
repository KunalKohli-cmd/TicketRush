package com.TicketMaster.user_service.service;

import com.TicketMaster.user_service.dto.ChangePasswordRequest;
import com.TicketMaster.user_service.dto.UpdateProfileRequest;
import com.TicketMaster.user_service.dto.UserResponse;
import com.TicketMaster.user_service.entity.User;
import com.TicketMaster.user_service.exceptions.InvalidCredentialsException;
import com.TicketMaster.user_service.exceptions.UserNotFoundException;
import com.TicketMaster.user_service.mapper.UserMapper;
import com.TicketMaster.user_service.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse getMe(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        return UserMapper.toResponse(user);
    }

    public UserResponse updateProfile(Long userId, UpdateProfileRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        user.setName(req.name());
        userRepository.save(user);
        return UserMapper.toResponse(user);
    }

    public void changePassword(Long userId, ChangePasswordRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        if (!passwordEncoder.matches(req.currentPassword(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }
        user.setPassword(passwordEncoder.encode(req.newPassword()));
        userRepository.save(user);
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        return UserMapper.toResponse(user);
    }
}
