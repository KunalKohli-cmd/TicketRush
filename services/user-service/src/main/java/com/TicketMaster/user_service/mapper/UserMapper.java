package com.TicketMaster.user_service.mapper;

import com.TicketMaster.user_service.dto.UserResponse;
import com.TicketMaster.user_service.entity.User;

public class UserMapper {

    private UserMapper() {
        // utility class, no instantiation
    }

    public static UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole()
        );
    }
}
