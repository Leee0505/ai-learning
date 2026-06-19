package com.ticket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ticket.common.constant.BusinessConstants;
import com.ticket.common.constant.ErrorCode;
import com.ticket.common.constant.RoleConstants;
import com.ticket.common.exception.BusinessException;
import com.ticket.dto.request.*;
import com.ticket.dto.response.PageResponse;
import com.ticket.dto.response.UserResponse;
import com.ticket.entity.User;
import com.ticket.mapper.UserMapper;
import com.ticket.service.AdminService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdminServiceImpl implements AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminServiceImpl.class);

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public AdminServiceImpl(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public UserResponse createUser(AdminCreateUserRequest request, Long adminId) {
        // Validate role
        if (!RoleConstants.ROLE_USER.equals(request.getRole())
                && !RoleConstants.ROLE_AGENT.equals(request.getRole())
                && !RoleConstants.ROLE_ADMIN.equals(request.getRole())) {
            throw new BusinessException(ErrorCode.ROLE_INVALID);
        }

        // Check uniqueness
        if (userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername())) > 0) {
            throw new BusinessException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }
        if (userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, request.getEmail())) > 0) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setStatus(BusinessConstants.USER_STATUS_ENABLED);
        user.setCreatedBy(adminId);
        userMapper.insert(user);

        log.info("Admin {} created user {} (id={}) with role {}", adminId, user.getUsername(), user.getId(), user.getRole());
        return UserResponse.from(user);
    }

    @Override
    public PageResponse<UserResponse> listUsers(UserListRequest request) {
        int page = request.getPage() != null ? request.getPage() : BusinessConstants.DEFAULT_PAGE;
        int size = request.getSize() != null ? request.getSize() : BusinessConstants.DEFAULT_SIZE;

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        // Keyword search — match username, email, or phone
        if (StringUtils.hasText(request.getKeyword())) {
            wrapper.and(w -> w
                .like(User::getUsername, request.getKeyword())
                .or()
                .like(User::getEmail, request.getKeyword())
                .or()
                .like(User::getPhone, request.getKeyword()));
        }

        // Filter by role
        if (StringUtils.hasText(request.getRole())) {
            wrapper.eq(User::getRole, request.getRole());
        }

        // Filter by status
        if (request.getStatus() != null) {
            wrapper.eq(User::getStatus, request.getStatus());
        }

        // Order by created date descending (newest first)
        wrapper.orderByDesc(User::getCreatedDate);

        IPage<User> result = userMapper.selectPage(Page.of(page, size), wrapper);
        return PageResponse.of(result, result.getRecords().stream().map(UserResponse::from).toList());
    }

    @Override
    public UserResponse getUserById(Long id) {
        User user = findUserOrThrow(id);
        return UserResponse.from(user);
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long id, UserUpdateRequest request, Long adminId) {
        User user = findUserOrThrow(id);

        boolean changed = false;

        if (StringUtils.hasText(request.getUsername()) && !request.getUsername().equals(user.getUsername())) {
            // Check uniqueness
            Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                    .eq(User::getUsername, request.getUsername())
                    .ne(User::getId, id));
            if (count > 0) {
                throw new BusinessException(ErrorCode.USERNAME_ALREADY_EXISTS);
            }
            user.setUsername(request.getUsername());
            changed = true;
        }

        if (StringUtils.hasText(request.getEmail()) && !request.getEmail().equals(user.getEmail())) {
            Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                    .eq(User::getEmail, request.getEmail())
                    .ne(User::getId, id));
            if (count > 0) {
                throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
            }
            user.setEmail(request.getEmail());
            changed = true;
        }

        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
            changed = true;
        }

        if (changed) {
            userMapper.updateById(user);
            log.info("Admin {} updated user {} (id={})", adminId, user.getUsername(), id);
        }

        return UserResponse.from(user);
    }

    @Override
    @Transactional
    public void deleteUser(Long id, Long adminId) {
        if (id.equals(adminId)) {
            throw new BusinessException(ErrorCode.CANNOT_DELETE_SELF);
        }

        User user = findUserOrThrow(id);
        userMapper.deleteById(id);
        log.info("Admin {} deleted user {} (id={})", adminId, user.getUsername(), id);
    }

    @Override
    @Transactional
    public UserResponse changeRole(Long id, UserRoleUpdateRequest request, Long adminId) {
        if (id.equals(adminId)) {
            throw new BusinessException(ErrorCode.CANNOT_CHANGE_OWN_ROLE);
        }

        // Validate role
        if (!RoleConstants.ROLE_USER.equals(request.getRole())
                && !RoleConstants.ROLE_AGENT.equals(request.getRole())
                && !RoleConstants.ROLE_ADMIN.equals(request.getRole())) {
            throw new BusinessException(ErrorCode.ROLE_INVALID);
        }

        User user = findUserOrThrow(id);
        String oldRole = user.getRole();
        user.setRole(request.getRole());
        userMapper.updateById(user);

        log.info("Admin {} changed user {} (id={}) role from {} to {}", adminId, user.getUsername(), id, oldRole, request.getRole());
        return UserResponse.from(user);
    }

    @Override
    @Transactional
    public UserResponse updateStatus(Long id, UserStatusUpdateRequest request, Long adminId) {
        if (id.equals(adminId)) {
            throw new BusinessException(ErrorCode.CANNOT_DISABLE_SELF);
        }

        int newStatus;
        try {
            newStatus = Integer.parseInt(request.getStatus());
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "status must be 0 or 1");
        }
        if (newStatus != 0 && newStatus != 1) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "status must be 0 or 1");
        }

        User user = findUserOrThrow(id);
        user.setStatus(newStatus);
        userMapper.updateById(user);

        String action = newStatus == BusinessConstants.USER_STATUS_ENABLED ? "enabled" : "disabled";
        log.info("Admin {} {} user {} (id={})", adminId, action, user.getUsername(), id);
        return UserResponse.from(user);
    }

    private User findUserOrThrow(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }
}
