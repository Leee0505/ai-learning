package com.ticket.service;

import com.ticket.dto.request.*;
import com.ticket.dto.response.AuthResponse;
import com.ticket.dto.response.UserResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    void logout(String accessToken);
    AuthResponse refresh(RefreshRequest request);
    UserResponse getCurrentUser(Long userId);
    void changePassword(Long userId, ChangePasswordRequest request);
    String invite(InviteRequest request, Long adminId);
    AuthResponse acceptInvite(AcceptInviteRequest request);
}
