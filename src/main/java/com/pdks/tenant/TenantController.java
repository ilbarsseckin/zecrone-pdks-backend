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

    // ── SUPER_ADMIN ──────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<TenantRegistrationResult> create(@Valid @RequestBody CreateTenantDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(tenantService.register(dto));
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<Tenant>> list() {
        return ResponseEntity.ok(tenantService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Tenant> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(tenantService.findById(id));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> setStatus(@PathVariable UUID id, @RequestParam boolean active) {
        tenantService.setActive(id, active);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/plan")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Tenant> changePlan(@PathVariable UUID id, @RequestParam Tenant.Plan plan) {
        return ResponseEntity.ok(tenantService.changePlan(id, plan));
    }

    // ── ADMIN / MANAGER ──────────────────────────────────────────────────────

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    public ResponseEntity<Tenant> getMe() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(tenantService.findById(tenantId));
    }

    @PatchMapping("/me")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Tenant> updateMe(@Valid @RequestBody UpdateTenantDto dto) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(tenantService.update(tenantId, dto));
    }
}