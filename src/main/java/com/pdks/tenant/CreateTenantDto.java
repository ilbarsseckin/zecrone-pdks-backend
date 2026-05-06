package com.pdks.tenant;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateTenantDto {

    @NotBlank(message = "Firma adı zorunlu")
    private String companyName;

    @NotNull(message = "Plan seçilmeli")
    private Tenant.Plan plan;

    @NotBlank @Email(message = "Geçerli email girin")
    private String contactEmail;

    private String contactPhone;
    private String adminPassword;
}