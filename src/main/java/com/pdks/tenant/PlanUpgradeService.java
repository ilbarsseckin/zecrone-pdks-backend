package com.pdks.tenant;

import com.pdks.common.BusinessException;
import com.pdks.config.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlanUpgradeService {

    private final PlanUpgradeRepository upgradeRepo;
    private final TenantService         tenantService;
    private final TenantRepository      tenantRepo;

    /**
     * ADMIN plan yükseltme talebi oluşturur.
     * Aynı anda sadece 1 bekleyen talep olabilir.
     */
    @Transactional
    public PlanUpgradeRequest requestUpgrade(PlanUpgradeDto dto) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null)
            throw new BusinessException("Oturum bilgisi alınamadı");

        Tenant tenant = tenantService.findById(tenantId);

        // Zaten bekleyen talep var mı?
        upgradeRepo.findByTenantIdAndStatus(tenantId, PlanUpgradeRequest.Status.PENDING)
            .ifPresent(r -> { throw new BusinessException(
                "Zaten bekleyen bir plan yükseltme talebiniz var. " +
                "Lütfen sonucu bekleyin."); });

        // Aynı veya daha düşük plan seçilemez
        if (dto.getRequestedPlan().ordinal() <= tenant.getPlan().ordinal())
            throw new BusinessException(
                "Yalnızca mevcut planınızdan daha yüksek bir plan seçebilirsiniz.");

        PlanUpgradeRequest req = new PlanUpgradeRequest();
        req.setTenantId(tenantId);
        req.setCurrentPlan(tenant.getPlan());
        req.setRequestedPlan(dto.getRequestedPlan());
        req.setNote(dto.getNote());
        req.setStatus(PlanUpgradeRequest.Status.PENDING);

        req = upgradeRepo.save(req);
        log.info("Plan yükseltme talebi: {} | {} → {}",
            tenant.getCompanyName(), tenant.getPlan(), dto.getRequestedPlan());
        return req;
    }

    /**
     * SUPER_ADMIN talebi onaylar → plan hemen değişir.
     * İleride: bu metot ödeme webhook'u tarafından çağrılacak.
     */
    @Transactional
    public PlanUpgradeRequest approve(UUID requestId, UUID reviewerId, String reviewNote) {
        PlanUpgradeRequest req = findById(requestId);

        if (req.getStatus() != PlanUpgradeRequest.Status.PENDING)
            throw new BusinessException("Bu talep zaten işleme alınmış");

        // Planı değiştir
        tenantService.changePlan(req.getTenantId(), req.getRequestedPlan());

        req.setStatus(PlanUpgradeRequest.Status.APPROVED);
        req.setReviewedBy(reviewerId);
        req.setReviewNote(reviewNote);
        req = upgradeRepo.save(req);

        log.info("Plan yükseltme onaylandı: talep={} yeni plan={}",
            requestId, req.getRequestedPlan());
        return req;
    }

    /**
     * SUPER_ADMIN talebi reddeder.
     */
    @Transactional
    public PlanUpgradeRequest reject(UUID requestId, UUID reviewerId, String reviewNote) {
        PlanUpgradeRequest req = findById(requestId);

        if (req.getStatus() != PlanUpgradeRequest.Status.PENDING)
            throw new BusinessException("Bu talep zaten işleme alınmış");

        req.setStatus(PlanUpgradeRequest.Status.REJECTED);
        req.setReviewedBy(reviewerId);
        req.setReviewNote(reviewNote);
        req = upgradeRepo.save(req);

        log.info("Plan yükseltme reddedildi: talep={}", requestId);
        return req;
    }

    /** Bekleyen tüm talepler — SUPER_ADMIN paneli için */
    public List<PlanUpgradeRequest> findAllPending() {
        return upgradeRepo.findAllByStatus(PlanUpgradeRequest.Status.PENDING);
    }

    /** Kendi talep geçmişi */
    public List<PlanUpgradeRequest> findMyRequests() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null)
            throw new BusinessException("Oturum bilgisi alınamadı");
        return upgradeRepo.findAllByTenantId(tenantId);
    }

    private PlanUpgradeRequest findById(UUID id) {
        return upgradeRepo.findById(id)
            .orElseThrow(() -> new BusinessException("Talep bulunamadı: " + id));
    }
}
