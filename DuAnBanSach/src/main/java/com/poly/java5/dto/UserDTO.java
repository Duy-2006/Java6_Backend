package com.poly.java5.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserDTO {
    private Integer id;
    private String username;
    private String email;
    private String fullName;
    private String phone;
    private String avatar;
    private Boolean active;
    private LocalDateTime createdAt;
}