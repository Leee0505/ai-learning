package com.ticket.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ticket.entity.User;
import com.ticket.mapper.UserMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserMapper userMapper;

    public UserDetailsServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        // Auto-detect: contains '@' → query by email, otherwise → query by username
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (login.contains("@")) {
            wrapper.eq(User::getEmail, login);
        } else {
            wrapper.eq(User::getUsername, login);
        }

        User user = userMapper.selectOne(wrapper);
        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + login);
        }
        return new UserDetailsImpl(user);
    }
}
