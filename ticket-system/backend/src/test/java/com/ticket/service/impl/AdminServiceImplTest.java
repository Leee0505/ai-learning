package com.ticket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ticket.common.constant.ErrorCode;
import com.ticket.common.constant.RoleConstants;
import com.ticket.common.exception.BusinessException;
import com.ticket.dto.request.*;
import com.ticket.dto.response.*;
import com.ticket.entity.User;
import com.ticket.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AdminServiceImplTest {

    private AdminServiceImpl service;
    private UserMapper userMapper;
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userMapper = mock(UserMapper.class);
        passwordEncoder = mock(PasswordEncoder.class);
        service = new AdminServiceImpl(userMapper, passwordEncoder);
    }

    private User createUser(Long id, String username, String role, Long tenantId) {
        User u = new User();
        u.setId(id); u.setUsername(username); u.setRole(role);
        u.setTenantId(tenantId); u.setPassword("enc"); u.setStatus(1);
        return u;
    }

    // ── Create User ──

    @Test
    void createUserShouldSucceed() {
        AdminCreateUserRequest req = new AdminCreateUserRequest();
        req.setUsername("newuser"); req.setEmail("new@test.com");
        req.setPassword("pass123"); req.setRole(RoleConstants.ROLE_USER);
        req.setTenantId(1L);

        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(passwordEncoder.encode("pass123")).thenReturn("encoded");
        when(userMapper.insert(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(200L);
            return 1;
        });

        UserResponse resp = service.createUser(req, 1L);

        assertThat(resp.getId()).isEqualTo(200L);
        assertThat(resp.getUsername()).isEqualTo("newuser");
        verify(userMapper).insert(any(User.class));
    }

    @Test
    void createUserShouldThrowWhenUsernameExists() {
        AdminCreateUserRequest req = new AdminCreateUserRequest();
        req.setUsername("existing"); req.setPassword("pass");
        req.setRole(RoleConstants.ROLE_USER); req.setTenantId(1L);

        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        assertThatThrownBy(() -> service.createUser(req, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.USERNAME_ALREADY_EXISTS);
    }

    // ── List Users ──

    @Test
    void listUsersShouldSucceed() {
        UserListRequest req = new UserListRequest();
        req.setPage(1); req.setSize(20);

        Page<User> mpPage = new Page<>(1, 20);
        mpPage.setRecords(List.of(
                createUser(1L, "user1", RoleConstants.ROLE_USER, 1L),
                createUser(2L, "agent1", RoleConstants.ROLE_AGENT, 1L)
        ));
        mpPage.setTotal(2);

        when(userMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mpPage);

        PageResponse<UserResponse> resp = service.listUsers(req);

        assertThat(resp.getTotal()).isEqualTo(2);
        assertThat(resp.getRecords()).hasSize(2);
    }

    // ── Get User By ID ──

    @Test
    void getUserByIdShouldSucceed() {
        User user = createUser(1L, "testuser", RoleConstants.ROLE_USER, 1L);
        when(userMapper.selectById(1L)).thenReturn(user);

        UserResponse resp = service.getUserById(1L);

        assertThat(resp.getUsername()).isEqualTo("testuser");
    }

    @Test
    void getUserByIdShouldThrowWhenNotFound() {
        when(userMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> service.getUserById(999L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    // ── Update User ──

    @Test
    void updateUserShouldSucceed() {
        User user = createUser(1L, "oldname", RoleConstants.ROLE_USER, 1L);
        when(userMapper.selectById(1L)).thenReturn(user);
        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        UserUpdateRequest req = new UserUpdateRequest();
        req.setUsername("newname"); req.setEmail("new@test.com");

        assertThatCode(() -> service.updateUser(1L, req, 1L)).doesNotThrowAnyException();
        assertThat(user.getUsername()).isEqualTo("newname");
    }

    @Test
    void updateUserShouldThrowWhenUsernameTaken() {
        User user = createUser(1L, "oldname", RoleConstants.ROLE_USER, 1L);
        when(userMapper.selectById(1L)).thenReturn(user);
        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L); // conflict

        UserUpdateRequest req = new UserUpdateRequest();
        req.setUsername("taken");

        assertThatThrownBy(() -> service.updateUser(1L, req, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.USERNAME_ALREADY_EXISTS);
    }

    // ── Delete User ──

    @Test
    void deleteUserShouldThrowWhenSelfDelete() {
        assertThatThrownBy(() -> service.deleteUser(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CANNOT_DELETE_SELF);
    }

    @Test
    void deleteUserShouldSucceed() {
        User user = createUser(2L, "other", RoleConstants.ROLE_USER, 1L);
        when(userMapper.selectById(2L)).thenReturn(user);
        when(userMapper.deleteById(2L)).thenReturn(1);

        assertThatCode(() -> service.deleteUser(2L, 1L)).doesNotThrowAnyException();
        verify(userMapper).deleteById(2L);
    }

    // ── Change Role ──

    @Test
    void changeRoleShouldThrowWhenSelf() {
        assertThatThrownBy(() -> service.changeRole(1L,
                new UserRoleUpdateRequest() {{ setRole(RoleConstants.ROLE_ADMIN); }}, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CANNOT_CHANGE_OWN_ROLE);
    }

    @Test
    void changeRoleShouldSucceed() {
        User user = createUser(2L, "agent1", RoleConstants.ROLE_AGENT, 1L);
        when(userMapper.selectById(2L)).thenReturn(user);
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        UserRoleUpdateRequest req = new UserRoleUpdateRequest();
        req.setRole(RoleConstants.ROLE_ADMIN);

        assertThatCode(() -> service.changeRole(2L, req, 1L)).doesNotThrowAnyException();
        assertThat(user.getRole()).isEqualTo(RoleConstants.ROLE_ADMIN);
    }

    // ── Update Status ──

    @Test
    void updateStatusShouldThrowWhenSelf() {
        assertThatThrownBy(() -> service.updateStatus(1L,
                new UserStatusUpdateRequest() {{ setStatus("0"); }}, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CANNOT_DISABLE_SELF);
    }

    @Test
    void updateStatusShouldThrowWhenInvalidStatus() {
        User user = createUser(2L, "user1", RoleConstants.ROLE_USER, 1L);
        when(userMapper.selectById(2L)).thenReturn(user);

        UserStatusUpdateRequest req = new UserStatusUpdateRequest();
        req.setStatus("invalid");

        assertThatThrownBy(() -> service.updateStatus(2L, req, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_USER_STATUS);
    }

    @Test
    void updateStatusShouldSucceed() {
        User user = createUser(2L, "user1", RoleConstants.ROLE_USER, 1L);
        when(userMapper.selectById(2L)).thenReturn(user);
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        UserStatusUpdateRequest req = new UserStatusUpdateRequest();
        req.setStatus("0");

        assertThatCode(() -> service.updateStatus(2L, req, 1L)).doesNotThrowAnyException();
        assertThat(user.getStatus()).isEqualTo(0);
    }
}
