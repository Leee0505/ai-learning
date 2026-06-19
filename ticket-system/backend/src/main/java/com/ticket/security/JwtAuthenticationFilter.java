package com.ticket.security;

import com.ticket.common.constant.BusinessConstants;
import com.ticket.common.constant.SecurityConstants;
import com.ticket.common.exception.TokenBlacklistedException;
import com.ticket.common.exception.TokenExpiredException;
import com.ticket.entity.User;
import com.ticket.mapper.UserMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtTokenProvider jwtTokenProvider;
    private final UserMapper userMapper;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, UserMapper userMapper) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userMapper = userMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);

        if (StringUtils.hasText(token)) {
            try {
                Claims claims = jwtTokenProvider.validateToken(token);
                if (claims != null) {
                    Long userId = Long.parseLong(claims.getSubject());
                    String role = claims.get("role", String.class);

                    // Verify user is still enabled (handles admin disable after token issuance)
                    User user = userMapper.selectById(userId);
                    if (user != null && user.getStatus() != null && user.getStatus() == BusinessConstants.USER_STATUS_DISABLED) {
                        response.setContentType("application/json");
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.getWriter().write(
                                "{\"code\":40019,\"message\":\"user account is disabled\",\"data\":null}");
                        return;
                    }

                    UserDetailsImpl principal = new UserDetailsImpl(userId, role);
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    principal, null,
                                    Collections.singletonList(new SimpleGrantedAuthority(role)));
                    authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (TokenExpiredException | TokenBlacklistedException e) {
                response.setContentType("application/json");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                int code = (e instanceof TokenExpiredException) ? 40101 : 40102;
                response.getWriter().write(
                        "{\"code\":" + code + ",\"message\":\"" + e.getMessage() + "\",\"data\":null}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(SecurityConstants.BEARER_PREFIX)) {
            return bearerToken.substring(SecurityConstants.BEARER_PREFIX_LENGTH);
        }
        return null;
    }
}
