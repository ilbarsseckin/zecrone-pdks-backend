package com.pdks.superadmin;

import com.pdks.tenant.Tenant;
import com.pdks.tenant.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/superadmin")
@RequiredArgsConstructor
public class SuperAdminController {

    private final TenantRepository tenantRepo;

    @GetMapping("/tenants")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<Tenant>> getAllTenants() {
        return ResponseEntity.ok(tenantRepo.findAll());
    }

    @PatchMapping("/tenants/{id}/activate")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Tenant> activate(@PathVariable UUID id) {
        Tenant t = tenantRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Firma bulunamadı"));
        t.setIsActive(true);
        return ResponseEntity.ok(tenantRepo.save(t));
    }

    @PatchMapping("/tenants/{id}/deactivate")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Tenant> deactivate(@PathVariable UUID id) {
        Tenant t = tenantRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Firma bulunamadı"));
        t.setIsActive(false);
        return ResponseEntity.ok(tenantRepo.save(t));
    }

    @PatchMapping("/tenants/{id}/plan")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Tenant> changePlan(
            @PathVariable UUID id,
            @RequestParam Tenant.Plan plan) {
        Tenant t = tenantRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Firma bulunamadı"));
        t.setPlan(plan);
        return ResponseEntity.ok(tenantRepo.save(t));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> getStats() {
        long total    = tenantRepo.count();
        long active   = tenantRepo.findAll().stream().filter(Tenant::getIsActive).count();
        long starter  = tenantRepo.findAll().stream().filter(t -> t.getPlan() == Tenant.Plan.STARTER).count();
        long pro      = tenantRepo.findAll().stream().filter(t -> t.getPlan() == Tenant.Plan.PROFESSIONAL).count();
        long enterprise = tenantRepo.findAll().stream().filter(t -> t.getPlan() == Tenant.Plan.ENTERPRISE).count();

        return ResponseEntity.ok(java.util.Map.of(
                "totalTenants",      total,
                "activeTenants",     active,
                "starterCount",      starter,
                "professionalCount", pro,
                "enterpriseCount",   enterprise
        ));
    }
}