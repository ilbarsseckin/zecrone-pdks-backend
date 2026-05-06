package com.pdks.mobile;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class QrCheckInDto {
    /** QR koddan okunan branchId */
    @NotBlank
    public String branchId;
}
