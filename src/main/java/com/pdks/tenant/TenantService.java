package com.pdks.tenant;

import com.pdks.config.TenantFlywayConfig;
import com.pdks.common.BusinessException;
import com.pdks.notification.NotificationService;
import com.pdks.user.User;
import com.pdks.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantService {

    private final TenantRepository   tenantRepo;
    private final DataSource         dataSource;
    private final TenantFlywayConfig flywayConfig;
    private final UserRepository     userRepo;
    private final PasswordEncoder    passwordEncoder;
    private final NotificationService notificationService;
    // ── Kayıt ────────────────────────────────────────────────────────────────

    /**
     * Yeni firma kaydeder:
     * 1. Tenant satırı oluşturur
     * 2. PostgreSQL schema açar
     * 3. Flyway migration çalıştırır (tablolar)
     * 4. İlk ADMIN kullanıcıyı otomatik oluşturur
     *
     * İlk admin şifresi dto.adminPassword ile gelir.
     * Eğer boşsa "Pdks@2025!" varsayılan şifre atanır — ilk girişte değiştirilmeli.
     */
    @Transactional
    public TenantRegistrationResult register(CreateTenantDto dto) {
        if (tenantRepo.existsByCompanyName(dto.getCompanyName()))
            throw new BusinessException("Bu firma adı zaten kayıtlı: " + dto.getCompanyName());

        String schemaName = "schema_" + UUID.randomUUID()
            .toString().replace("-", "").substring(0, 12);

        // 1. Tenant oluştur
        Tenant tenant = new Tenant();
        tenant.setCompanyName(dto.getCompanyName());
        tenant.setSchemaName(schemaName);
        tenant.setPlan(dto.getPlan());
        tenant.setContactEmail(dto.getContactEmail());
        tenant.setContactPhone(dto.getContactPhone());
        tenant.setMaxBranches(resolveMaxBranches(dto.getPlan()));
        tenant.setMaxEmployees(resolveMaxEmployees(dto.getPlan()));
        tenant = tenantRepo.save(tenant);

        // 2. Schema aç
        createSchema(schemaName);

        // 3. Tablolar
        flywayConfig.migrateSchema(schemaName);

        // 4. İlk admin kullanıcı — tenant schema'sı aktif olduğunda oluşturulur
        String rawPassword = (dto.getAdminPassword() != null && !dto.getAdminPassword().isBlank())
            ? dto.getAdminPassword()
            : "Pdks@2025!";

        User admin = new User();
        admin.setEmail(dto.getContactEmail());
        admin.setPasswordHash(passwordEncoder.encode(rawPassword));
        admin.setRole(User.UserRole.ADMIN);
        admin.setIsActive(true);
        userRepo.save(admin);

        log.info("Yeni tenant oluşturuldu: {} → {} | admin: {}",
            tenant.getCompanyName(), schemaName, dto.getContactEmail());


        // Email gönder
        notificationService.sendWelcomeEmail(
                dto.getContactEmail(),
                tenant.getCompanyName(),
                rawPassword
        );
        return new TenantRegistrationResult(tenant, dto.getContactEmail(), rawPassword);
    }

    // ── Okuma ────────────────────────────────────────────────────────────────

    public Tenant findById(UUID id) {
        return tenantRepo.findById(id)
            .orElseThrow(() -> new BusinessException("Firma bulunamadı: " + id));
    }

    public List<Tenant> findAll() {
        return tenantRepo.findAll();
    }

    // ── Güncelleme ────────────────────────────────────────────────────────────

    @Transactional
    public Tenant update(UUID id, UpdateTenantDto dto) {
        Tenant tenant = findById(id);
        tenant.setCompanyName(dto.getCompanyName());
        tenant.setContactEmail(dto.getContactEmail());
        tenant.setContactPhone(dto.getContactPhone());
        return tenantRepo.save(tenant);
    }

    @Transactional
    public Tenant changePlan(UUID id, Tenant.Plan newPlan) {
        Tenant tenant = findById(id);
        Tenant.Plan oldPlan = tenant.getPlan();
        tenant.setPlan(newPlan);
        tenant.setMaxBranches(resolveMaxBranches(newPlan));
        tenant.setMaxEmployees(resolveMaxEmployees(newPlan));
        tenant = tenantRepo.save(tenant);
        log.info("Plan değiştirildi: {} | {} → {}", tenant.getCompanyName(), oldPlan, newPlan);
        return tenant;
    }

    @Transactional
    public void setActive(UUID id, boolean active) {
        Tenant tenant = findById(id);
        tenant.setIsActive(active);
        tenantRepo.save(tenant);
        log.info("Tenant {} durumu: {}", id, active ? "aktif" : "pasif");
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private void createSchema(String schemaName) {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE SCHEMA IF NOT EXISTS " + schemaName);
        } catch (Exception e) {
            throw new RuntimeException("Schema oluşturulamadı: " + schemaName, e);
        }
    }

    private int resolveMaxBranches(Tenant.Plan plan) {
        return plan.getMaxBranches();
    }

    private int resolveMaxEmployees(Tenant.Plan plan) {
        return plan.getMaxEmployees();
    }


}
