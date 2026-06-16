package com.ticket.service;

import com.ticket.dto.request.*;
import com.ticket.dto.response.PageResponse;
import com.ticket.dto.response.UserResponse;

public interface AdminService {
    PageResponse<UserResponse> listUsers(UserListRequest request);
    UserResponse getUserById(Long id);
    UserResponse createUser(AdminCreateUserRequest request, Long adminId);
    UserResponse updateUser(Long id, UserUpdateRequest request, Long adminId);
    void deleteUser(Long id, Long adminId);
    UserResponse changeRole(Long id, UserRoleUpdateRequest request, Long adminId);
    UserResponse updateStatus(Long id, UserStatusUpdateRequest request, Long adminId);
}
