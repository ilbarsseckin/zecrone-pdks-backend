package com.pdks.tenant;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Tenant kayıt sonucu.
 * adminEmail ve initialPassword sadece kayıt anında döner —
 * SUPER_ADMIN bu bilgiyi müşteriye iletir.
 * Sonraki isteklerde şifre bir daha görünmez.
 */
@Data
@AllArgsConstructor
public class TenantRegistrationResult {
    private Tenant tenant;
    private String adminEmail;
    private String initialPassword;
}
