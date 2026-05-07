package com.pdks.superadmin;

import com.pdks.branch.BranchRepository;
import com.pdks.config.TenantContext;
import com.pdks.employee.EmployeeRepository;
import com.pdks.tenant.Tenant;
import com.pdks.tenant.TenantRepository;
import com.pdks.user.User;
import com.pdks.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RestController
@RequestMapping("/api/superadmin")
@RequiredArgsConstructor
public class SuperAdminController {

    private final BranchRepository  branchRepo;
    private final EmployeeRepository employeeRepo;
    private final TenantRepository   tenantRepo;
    private final UserRepository     userRepo;
    private final PasswordEncoder    passwordEncoder;

    // ─── Tenant Listesi ──────────────────────────────────────────────────────

    @GetMapping("/tenants")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getAllTenants() {
        List<Tenant> tenants = tenantRepo.findAll();
        List<Map<String, Object>> result = new ArrayList<>();

        for (Tenant t : tenants) {
            Map<String, Object> item = new HashMap<>();
            item.put("id",                   t.getId());
            item.put("companyName",          t.getCompanyName());
            item.put("contactEmail",         t.getContactEmail());
            item.put("contactPhone",         t.getContactPhone());
            item.put("plan",                 t.getPlan());
            item.put("isActive",             t.getIsActive());
            item.put("schemaName",           t.getSchemaName());
            item.put("createdAt",            t.getCreatedAt());
            item.put("notes",                t.getNotes());
            item.put("trialEndsAt",          t.getTrialEndsAt());
            item.put("subscriptionEndsAt",   t.getSubscriptionEndsAt());

            // Kalan gün
            if (t.getSubscriptionEndsAt() != null) {
                long d = ChronoUnit.DAYS.between(LocalDateTime.now(), t.getSubscriptionEndsAt());
                item.put("daysLeft", d);
            } else if (t.getTrialEndsAt() != null) {
                long d = ChronoUnit.DAYS.between(LocalDateTime.now(), t.getTrialEndsAt());
                item.put("daysLeft", d);
                item.put("isTrial", true);
            } else {
                item.put("daysLeft", null);
            }

            // Şube + personel sayısı
            try {
                TenantContext.setTenant(t.getSchemaName());
                item.put("branchCount",   branchRepo.countByIsActiveTrue());
                item.put("employeeCount", employeeRepo.count());
            } catch (Exception e) {
                item.put("branchCount",   0);
                item.put("employeeCount", 0);
            } finally {
                TenantContext.clear();
            }

            // Son aktivite
            try {
                TenantContext.setTenant(t.getSchemaName());
                userRepo.findTopByOrderByLastLoginDesc()
                        .ifPresent(u -> item.put("lastActivity", u.getLastLogin()));
            } catch (Exception e) {
                item.put("lastActivity", null);
            } finally {
                TenantContext.clear();
            }

            // MRR
            int mrr = switch (t.getPlan()) {
                case STARTER      -> 499;
                case PROFESSIONAL -> 1499;
                case ENTERPRISE   -> 4999;
            };
            item.put("mrr", mrr);

            // Churn riski
            String risk = "LOW";
            if (!t.getIsActive()) {
                risk = "CHURNED";
            } else if (t.getSubscriptionEndsAt() != null) {
                long d = ChronoUnit.DAYS.between(LocalDateTime.now(), t.getSubscriptionEndsAt());
                if (d < 0)  risk = "EXPIRED";
                else if (d <= 3)  risk = "CRITICAL";
                else if (d <= 7)  risk = "HIGH";
                else if (d <= 30) risk = "MEDIUM";
            }
            item.put("churnRisk", risk);

            result.add(item);
        }

        return ResponseEntity.ok(result);
    }

    // ─── İstatistikler ───────────────────────────────────────────────────────

    @GetMapping("/stats")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> getStats() {
        List<Tenant> all = tenantRepo.findAll();

        long total      = all.size();
        long active     = all.stream().filter(Tenant::getIsActive).count();
        long starter    = all.stream().filter(t -> t.getPlan() == Tenant.Plan.STARTER).count();
        long pro        = all.stream().filter(t -> t.getPlan() == Tenant.Plan.PROFESSIONAL).count();
        long enterprise = all.stream().filter(t -> t.getPlan() == Tenant.Plan.ENTERPRISE).count();

        int mrr = all.stream().filter(Tenant::getIsActive).mapToInt(t -> switch (t.getPlan()) {
            case STARTER      -> 499;
            case PROFESSIONAL -> 1499;
            case ENTERPRISE   -> 4999;
        }).sum();

        long newThisMonth = all.stream().filter(t ->
                t.getCreatedAt() != null &&
                        t.getCreatedAt().getMonth() == LocalDateTime.now().getMonth() &&
                        t.getCreatedAt().getYear()  == LocalDateTime.now().getYear()
        ).count();

        long expiringSoon = all.stream().filter(t ->
                t.getSubscriptionEndsAt() != null &&
                        ChronoUnit.DAYS.between(LocalDateTime.now(), t.getSubscriptionEndsAt()) <= 7 &&
                        ChronoUnit.DAYS.between(LocalDateTime.now(), t.getSubscriptionEndsAt()) >= 0
        ).count();

        // Son 6 ay aylık kayıt sayısı (grafik için)
        List<Map<String, Object>> monthlyGrowth = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            LocalDateTime month = LocalDateTime.now().minusMonths(i);
            long count = all.stream().filter(t ->
                    t.getCreatedAt() != null &&
                            t.getCreatedAt().getMonth() == month.getMonth() &&
                            t.getCreatedAt().getYear()  == month.getYear()
            ).count();
            monthlyGrowth.add(Map.of(
                    "month", month.getMonth().getDisplayName(
                            java.time.format.TextStyle.SHORT,
                            new java.util.Locale("tr")),
                    "count", count,
                    "mrr", count * 499
            ));
        }

        return ResponseEntity.ok(Map.of(
                "totalTenants",      total,
                "activeTenants",     active,
                "starterCount",      starter,
                "professionalCount", pro,
                "enterpriseCount",   enterprise,
                "monthlyRevenue",    mrr,
                "newThisMonth",      newThisMonth,
                "expiringSoon",      expiringSoon,
                "monthlyGrowth",     monthlyGrowth
        ));
    }

    // ─── Aktifleştir / Askıya Al ─────────────────────────────────────────────

    @PatchMapping("/tenants/{id}/activate")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Tenant> activate(@PathVariable UUID id) {
        Tenant t = tenantRepo.findById(id).orElseThrow();
        t.setIsActive(true);
        return ResponseEntity.ok(tenantRepo.save(t));
    }

    @PatchMapping("/tenants/{id}/deactivate")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Tenant> deactivate(@PathVariable UUID id) {
        Tenant t = tenantRepo.findById(id).orElseThrow();
        t.setIsActive(false);
        return ResponseEntity.ok(tenantRepo.save(t));
    }

    // ─── Plan Değiştir ───────────────────────────────────────────────────────

    @PatchMapping("/tenants/{id}/plan")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Tenant> changePlan(
            @PathVariable UUID id,
            @RequestParam Tenant.Plan plan) {
        Tenant t = tenantRepo.findById(id).orElseThrow();
        t.setPlan(plan);
        return ResponseEntity.ok(tenantRepo.save(t));
    }

    // ─── Abonelik Yönetimi ───────────────────────────────────────────────────

    @PatchMapping("/tenants/{id}/subscription")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Tenant> setSubscription(
            @PathVariable UUID id,
            @RequestParam(required = false) String endsAt,
            @RequestParam(required = false) String trialEndsAt,
            @RequestParam(required = false) String notes) {
        Tenant t = tenantRepo.findById(id).orElseThrow();
        if (endsAt != null && !endsAt.isBlank())
            t.setSubscriptionEndsAt(LocalDateTime.parse(endsAt + "T00:00:00"));
        if (trialEndsAt != null && !trialEndsAt.isBlank())
            t.setTrialEndsAt(LocalDateTime.parse(trialEndsAt + "T00:00:00"));
        if (notes != null)
            t.setNotes(notes);
        return ResponseEntity.ok(tenantRepo.save(t));
    }

    // ─── Super Admin Kullanıcı Yönetimi ──────────────────────────────────────

    @GetMapping("/admins")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> getAdmins() {
        TenantContext.setTenant("public");
        try {
            var admins = userRepo.findAll().stream()
                    .filter(u -> u.getRole() == User.UserRole.SUPER_ADMIN)
                    .map(u -> Map.of(
                            "id",        u.getId(),
                            "email",     u.getEmail(),
                            "isActive",  u.getIsActive(),
                            "lastLogin", u.getLastLogin() != null ? u.getLastLogin().toString() : ""
                    ))
                    .toList();
            return ResponseEntity.ok(admins);
        } finally {
            TenantContext.clear();
        }
    }

    @PostMapping("/admins")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> createAdmin(@RequestBody Map<String, String> body) {
        TenantContext.setTenant("public");
        try {
            String email    = body.get("email");
            String password = body.get("password");

            if (email == null || password == null)
                return ResponseEntity.badRequest().body(Map.of("error", "Email ve şifre zorunlu"));

            if (userRepo.existsByEmail(email))
                return ResponseEntity.badRequest().body(Map.of("error", "Bu email zaten kayıtlı"));

            User u = new User();
            u.setEmail(email);
            u.setPasswordHash(passwordEncoder.encode(password));
            u.setRole(User.UserRole.SUPER_ADMIN);
            u.setIsActive(true);
            userRepo.save(u);

            return ResponseEntity.ok(Map.of("message", "Admin oluşturuldu", "email", email));
        } finally {
            TenantContext.clear();
        }
    }

    @DeleteMapping("/admins/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> deleteAdmin(@PathVariable UUID id) {
        TenantContext.setTenant("public");
        try {
            userRepo.deleteById(id);
            return ResponseEntity.noContent().build();
        } finally {
            TenantContext.clear();
        }
    }
}