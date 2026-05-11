package com.pdks.auth.verification;

import com.pdks.common.BusinessException;
import com.pdks.notification.NotificationService;
import com.pdks.tenant.Tenant;
import com.pdks.tenant.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationService {

    private final EmailVerificationTokenRepository tokenRepo;
    private final TenantRepository                 tenantRepo;
    private final NotificationService              notificationService;

    /**
     * Yeni firma kaydında çağrılır.
     * Token üretir ve aktivasyon mailini gönderir.
     */
    @Transactional
    public void sendVerificationEmail(UUID tenantId, String email, String companyName) {
        // Eski tokenları temizle
        tokenRepo.deleteAllByTenantId(tenantId);

        // Yeni token oluştur (24 saat geçerli)
        EmailVerificationToken evToken = EmailVerificationToken.builder()
                .tenantId(tenantId)
                .email(email)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();

        evToken = tokenRepo.save(evToken);

        log.info("Aktivasyon tokeni oluşturuldu: tenantId={} email={}", tenantId, email);

        // Aktivasyon mailini gönder
        notificationService.sendVerificationEmail(email, companyName, evToken.getToken());
    }

    /**
     * Kullanıcı link'e tıklayınca çağrılır.
     * Token geçerliyse tenant'ı verified yapar ve trial'ı başlatır.
     */
    @Transactional
    public void verifyEmail(UUID token) {
        EmailVerificationToken evToken = tokenRepo.findByToken(token)
                .orElseThrow(() -> new BusinessException("Geçersiz aktivasyon linki."));

        if (evToken.isUsed()) {
            throw new BusinessException("Bu aktivasyon linki daha önce kullanılmış.");
        }

        if (evToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Aktivasyon linkinin süresi dolmuş. Yeni link talep edin.");
        }

        // Token'ı kullanıldı işaretle
        evToken.setUsed(true);
        evToken.setUsedAt(LocalDateTime.now());
        tokenRepo.save(evToken);

        // Tenant'ı aktifleştir ve 14 günlük trial başlat
        Tenant tenant = tenantRepo.findById(evToken.getTenantId())
                .orElseThrow(() -> new BusinessException("Firma bulunamadı."));

        tenant.setVerified(true);
        tenant.setVerifiedAt(LocalDateTime.now());
        tenant.setTrialStartedAt(LocalDateTime.now());
        tenant.setTrialEndsAt(LocalDateTime.now().plusDays(14));
        tenantRepo.save(tenant);

        log.info("E-posta doğrulandı: tenantId={} email={} trial bitiş={}",
                tenant.getId(), evToken.getEmail(), tenant.getTrialEndsAt());
    }

    /**
     * Süresi dolan token için yeni link gönderir.
     */
    @Transactional
    public void resendVerificationEmail(String email) {
        Tenant tenant = tenantRepo.findByContactEmail(email)
                .orElseThrow(() -> new BusinessException("Bu e-posta ile kayıtlı firma bulunamadı."));

        if (Boolean.TRUE.equals(tenant.getVerified())) {
            throw new BusinessException("Bu hesap zaten doğrulanmış.");
        }

        sendVerificationEmail(tenant.getId(), email, tenant.getCompanyName());
        log.info("Aktivasyon maili yeniden gönderildi: {}", email);
    }
}
