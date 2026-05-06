package com.pdks.branch;

import com.pdks.common.BusinessException;
import com.pdks.config.TenantContext;
import com.pdks.tenant.Tenant;
import com.pdks.tenant.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BranchService {

    private final BranchRepository branchRepo;
    private final TenantRepository tenantRepo;

    public List<Branch> findAll() {
        return branchRepo.findAllByIsActiveTrue();
    }

    public Branch findById(UUID id) {
        return branchRepo.findById(id)
            .orElseThrow(() -> new BusinessException("Şube bulunamadı: " + id));
    }

    @Transactional
    public Branch create(BranchDto dto) {
        // ── Plan limit kontrolü ──────────────────────────────────────────────
        checkBranchLimit();

        if (branchRepo.existsByName(dto.getName()))
            throw new BusinessException("Bu isimde şube zaten var: " + dto.getName());

        Branch branch = new Branch();
        branch.setName(dto.getName());
        branch.setCity(dto.getCity());
        branch.setAddress(dto.getAddress());
        branch.setPhone(dto.getPhone());
        branch.setWorkStart(dto.getWorkStart());
        branch.setWorkEnd(dto.getWorkEnd());
        branch.setLateTolerance(dto.getLateTolerance());
        branch.setTimezone(dto.getTimezone());

        return branchRepo.save(branch);
    }

    @Transactional
    public Branch update(UUID id, BranchDto dto) {
        Branch branch = findById(id);
        branch.setName(dto.getName());
        branch.setCity(dto.getCity());
        branch.setAddress(dto.getAddress());
        branch.setPhone(dto.getPhone());
        branch.setWorkStart(dto.getWorkStart());
        branch.setWorkEnd(dto.getWorkEnd());
        branch.setLateTolerance(dto.getLateTolerance());
        branch.setTimezone(dto.getTimezone());
        return branchRepo.save(branch);
    }

    @Transactional
    public void delete(UUID id) {
        Branch branch = findById(id);
        branch.setIsActive(false);
        branchRepo.save(branch);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    /**
     * Aktif tenant'ın şube limitini kontrol eder.
     * TenantContext.getTenantId() JwtFilter tarafından her istekte doldurulur.
     */
    private void checkBranchLimit() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) return; // Test ortamı veya SUPER_ADMIN isteği

        Tenant tenant = tenantRepo.findById(tenantId)
            .orElseThrow(() -> new BusinessException("Tenant bulunamadı"));

        long current = branchRepo.countByIsActiveTrue();

        if (current >= tenant.getMaxBranches()) {
            throw new BusinessException(String.format(
                "Şube limitinize ulaştınız. Planınız en fazla %d şubeye izin veriyor. " +
                "Daha fazla şube eklemek için planınızı yükseltin.",
                tenant.getMaxBranches()
            ));
        }
    }
}
