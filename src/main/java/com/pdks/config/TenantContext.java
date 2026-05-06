package com.pdks.config;

import java.util.UUID;

/**
 * Her HTTP isteği için aktif tenant bilgisini ThreadLocal'de tutar.
 * JwtFilter tarafından doldurulur, istek sonunda clear() ile temizlenir.
 */
public class TenantContext {

    private static final ThreadLocal<String> currentSchema   = new ThreadLocal<>();
    private static final ThreadLocal<UUID>   currentTenantId = new ThreadLocal<>();

    // ── Schema (multi-tenant DB yönlendirmesi için) ──────────────────────────

    public static void setTenant(String schemaName) {
        currentSchema.set(schemaName);
    }

    public static String getTenant() {
        return currentSchema.get();
    }

    // ── Tenant ID (limit kontrolü ve /me endpoint'i için) ────────────────────

    public static void setTenantId(UUID tenantId) {
        currentTenantId.set(tenantId);
    }

    public static UUID getTenantId() {
        return currentTenantId.get();
    }

    // ── Temizlik (JwtFilter finally bloğunda çağrılır) ───────────────────────

    public static void clear() {
        currentSchema.remove();
        currentTenantId.remove();
    }
}
