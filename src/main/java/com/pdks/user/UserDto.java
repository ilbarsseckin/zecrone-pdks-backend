package com.pdks.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class UserDto {

    @NotBlank @Email
    private String email;

    @NotBlank @Size(min = 8)
    private String password;

    private User.UserRole role = User.UserRole.STAFF;
    private UUID branchId;
    private UUID employeeId;
}