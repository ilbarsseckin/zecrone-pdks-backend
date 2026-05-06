package com.pdks.mobile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MobileChangePasswordDto {
    @NotBlank
    public String currentPassword;

    @NotBlank
    @Size(min = 6, message = "Yeni şifre en az 6 karakter olmalıdır")
    public String newPassword;
}
