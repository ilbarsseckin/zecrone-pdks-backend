package com.pdks.tenant;

import com.pdks.config.TenantContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    // ── SUPER_ADMIN endpoint'leri ────────────────────────────────────────────

    /** Yeni firma kaydı — sadece SUPER_ADMIN */
    @PostMapping
   // @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<TenantRegistrationResult> create(@Valid @RequestBody CreateTenantDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(tenantService.register(dto));
    }

    /** Tüm firmalar — sadece SUPER_ADMIN */
    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<Tenant>> list() {
        return ResponseEntity.ok(tenantService.findAll());
    }

    /** Belirli firma — sadece SUPER_ADMIN */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Tenant> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(tenantService.findById(id));
    }

    /** Firma aktif/pasif yap — sadece SUPER_ADMIN */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> setStatus(
            @PathVariable UUID id,
            @RequestParam boolean active) {
        tenantService.setActive(id, active);
        return ResponseEntity.noContent().build();
    }

    /** Plan değiştir — sadece SUPER_ADMIN (ödeme sistemi entegrasyonuna kadar) */
    @PatchMapping("/{id}/plan")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Tenant> changePlan(
            @PathVariable UUID id,
            @RequestParam Tenant.Plan plan) {
        return ResponseEntity.ok(tenantService.changePlan(id, plan));
    }

    // ── ADMIN / MANAGER endpoint'leri ────────────────────────────────────────

    /**
     * Giriş yapan kullanıcının kendi tenant bilgisini döner.
     * Settings sayfası ve plan/limit gösterimi için kullanılır.
     * TenantContext.getTenantId() JwtFilter tarafından her istekte doldurulur.
     */
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Tenant> getMe() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(tenantService.findById(tenantId));
    }

    /**
     * Firma iletişim bilgilerini güncelle.
     * ADMIN kendi firmasının bilgilerini düzenleyebilir.
     */
    @PatchMapping("/me")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Tenant> updateMe(@Valid @RequestBody UpdateTenantDto dto) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(tenantService.update(tenantId, dto));
    }
}
