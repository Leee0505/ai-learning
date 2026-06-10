package com.ticket.dto.response;

import com.ticket.entity.User;

public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String phone;
    private String role;
    private Integer status;
    private Long createdDate;

    public static UserResponse from(User user) {
        UserResponse response = new UserResponse();
        response.id = user.getId();
        response.username = user.getUsername();
        response.email = user.getEmail();
        response.phone = user.getPhone();
        response.role = user.getRole();
        response.status = user.getStatus();
        response.createdDate = user.getCreatedDate();
        return response;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getRole() { return role; }
    public Integer getStatus() { return status; }
    public Long getCreatedDate() { return createdDate; }
}
