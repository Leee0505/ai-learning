package com.ticket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ticket.common.constant.BusinessConstants;
import com.ticket.common.constant.ErrorCode;
import com.ticket.common.constant.RoleConstants;
import com.ticket.common.exception.*;
import com.ticket.dto.request.*;
import com.ticket.dto.response.AuthResponse;
import com.ticket.dto.response.UserResponse;
import com.ticket.entity.InviteToken;
import com.ticket.entity.User;
import com.ticket.mapper.InviteTokenMapper;
import com.ticket.mapper.UserMapper;
import com.ticket.security.JwtTokenProvider;
import com.ticket.security.UserDetailsImpl;
import com.ticket.service.AuthService;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserMapper userMapper;
    private final InviteTokenMapper inviteTokenMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthServiceImpl(UserMapper userMapper, InviteTokenMapper inviteTokenMapper,
                           PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager,
                           JwtTokenProvider jwtTokenProvider) {
        this.userMapper = userMapper;
        this.inviteTokenMapper = inviteTokenMapper;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Check uniqueness
        if (userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername())) > 0) {
            throw new UsernameAlreadyExistsException();
        }
        if (userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, request.getEmail())) > 0) {
            throw new EmailAlreadyExistsException();
        }

        // Create user
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(RoleConstants.ROLE_USER);
        user.setStatus(BusinessConstants.USER_STATUS_ENABLED);
        userMapper.insert(user);

        // Generate tokens
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getRole(), user.getTenantId());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        log.info("User registered: {} (id={})", user.getUsername(), user.getId());
        return new AuthResponse(accessToken, refreshToken, UserResponse.from(user));
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        // Authenticate
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getLogin(), request.getPassword()));

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        // Load full user
        User user = userMapper.selectById(userDetails.getUserId());

        // Generate tokens
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getRole(), user.getTenantId());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        log.info("User logged in: {} (id={})", user.getUsername(), user.getId());
        return new AuthResponse(accessToken, refreshToken, UserResponse.from(user));
    }

    @Override
    public void logout(String accessToken) {
        jwtTokenProvider.blacklistAccessToken(accessToken);
        log.info("User logged out");
    }

    @Override
    public AuthResponse refresh(RefreshRequest request) {
        String refreshToken = request.getRefreshToken();

        // Validate refresh token
        Claims claims = jwtTokenProvider.validateToken(refreshToken);
        if (claims == null || claims.getId() == null) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }

        Long userId = Long.parseLong(claims.getSubject());
        String jti = claims.getId();

        // Verify token is in whitelist
        if (!jwtTokenProvider.isRefreshTokenValid(userId, jti)) {
            throw new BusinessException(ErrorCode.TOKEN_BLACKLISTED);
        }

        // Revoke old refresh token (rotation)
        jwtTokenProvider.revokeRefreshToken(userId, jti);

        // Issue new tokens
        User user = userMapper.selectById(userId);
        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getRole(), user.getTenantId());
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        return new AuthResponse(newAccessToken, newRefreshToken, UserResponse.from(user));
    }

    @Override
    public UserResponse getCurrentUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return UserResponse.from(user);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        // Verify current password
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "current password is incorrect");
        }
        // Update to new password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(user);
        log.info("Password changed for user {}", userId);
    }

    @Override
    @Transactional
    public String invite(InviteRequest request, Long adminId) {
        String token = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();

        InviteToken inviteToken = new InviteToken();
        inviteToken.setToken(token);
        inviteToken.setEmail(request.getEmail());
        inviteToken.setExpiresAt(now + BusinessConstants.INVITE_EXPIRY_MS);
        inviteToken.setUsed(BusinessConstants.INVITE_TOKEN_UNUSED);
        inviteToken.setCreatedBy(adminId);
        inviteTokenMapper.insert(inviteToken);

        log.info("Admin {} invited {}", adminId, request.getEmail());
        return token;
    }

    @Override
    @Transactional
    public AuthResponse acceptInvite(AcceptInviteRequest request) {
        // Find token
        InviteToken inviteToken = inviteTokenMapper.selectOne(
                new LambdaQueryWrapper<InviteToken>()
                        .eq(InviteToken::getToken, request.getToken()));

        if (inviteToken == null) {
            throw new InviteTokenNotFoundException();
        }
        if (inviteToken.getUsed() == BusinessConstants.INVITE_TOKEN_USED) {
            throw new InviteTokenUsedException();
        }
        if (System.currentTimeMillis() > inviteToken.getExpiresAt()) {
            throw new InviteTokenExpiredException();
        }

        // Check username uniqueness
        if (userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername())) > 0) {
            throw new UsernameAlreadyExistsException();
        }

        // Create agent user
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(inviteToken.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(RoleConstants.ROLE_AGENT);
        user.setStatus(BusinessConstants.USER_STATUS_ENABLED);
        userMapper.insert(user);

        // Mark token as used
        inviteToken.setUsed(BusinessConstants.INVITE_TOKEN_USED);
        inviteTokenMapper.updateById(inviteToken);

        // Generate tokens
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getRole(), user.getTenantId());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        log.info("Agent {} accepted invite from email {}", user.getUsername(), inviteToken.getEmail());
        return new AuthResponse(accessToken, refreshToken, UserResponse.from(user));
    }
}
