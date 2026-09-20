package com.TicketMaster.user_service.controller;

import com.TicketMaster.user_service.dto.ChangePasswordRequest;
import com.TicketMaster.user_service.dto.UpdateProfileRequest;
import com.TicketMaster.user_service.dto.UserResponse;
import com.TicketMaster.user_service.security.CustomUserDetails;
import com.TicketMaster.user_service.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(Authentication authentication) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(userService.getMe(userId));
    }

    @PatchMapping("/me")
    public ResponseEntity<UserResponse> updateProfile(Authentication authentication,
                                                       @Valid @RequestBody UpdateProfileRequest req) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(userService.updateProfile(userId, req));
    }

    @PostMapping("/me/password")
    public ResponseEntity<Void> changePassword(Authentication authentication,
                                                @Valid @RequestBody ChangePasswordRequest req) {
        Long userId = extractUserId(authentication);
        userService.changePassword(userId, req);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    private Long extractUserId(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return userDetails.getId();
    }
}
