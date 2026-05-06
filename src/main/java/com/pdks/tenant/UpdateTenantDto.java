package com.pdks.tenant;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * PATCH /api/tenants/me — firma iletişim bilgilerini günceller.
 * Plan ve limitler bu DTO ile değiştirilemez.
 */
@Data
public class UpdateTenantDto {

    @NotBlank(message = "Firma adı zorunlu")
    private String companyName;

    @Email(message = "Geçerli bir e-posta adresi girin")
    private String contactEmail;

    private String contactPhone;
}
