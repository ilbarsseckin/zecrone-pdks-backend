package com.pdks.mobile;

import lombok.Data;
import java.util.UUID;

@Data
public class MobileAuthResponseDto {
    public String token;
    public UUID   employeeId;
    public String fullName;
    public UUID   branchId;
    public String branchName;
    public long   expiresIn = 86400;
}
