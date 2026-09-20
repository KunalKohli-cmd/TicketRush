package com.TicketMaster.user_service.dto;

import com.TicketMaster.user_service.entity.Role;

public record UserResponse(Long id, String name, String email, Role role) {}
