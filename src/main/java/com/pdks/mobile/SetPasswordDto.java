package com.pdks.mobile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SetPasswordDto {
    /** İlk kez şifre belirlemek için */
    @NotBlank
    @Size(min = 6)
    public String password;
}
