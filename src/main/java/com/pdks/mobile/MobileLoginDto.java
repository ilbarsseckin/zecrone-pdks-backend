package com.pdks.mobile;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MobileLoginDto {

    @NotBlank @Email
    public String tenantEmail;

    @NotBlank @Email
    public String employeeEmail;

    @NotBlank
    public String password;

    /** Cihaz push token — giriş sırasında kaydedilir */
    public String pushToken;
}
