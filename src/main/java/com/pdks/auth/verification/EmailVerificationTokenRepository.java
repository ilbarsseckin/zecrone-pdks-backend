package com.pdks.auth.verification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

public interface EmailVerificationTokenRepository
        extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByToken(UUID token);

    // Kullanılmamış ve süresi dolmamış token var mı?
    @Query("""
        SELECT COUNT(t) > 0 FROM EmailVerificationToken t
        WHERE t.tenantId = :tenantId
          AND t.used = false
          AND t.expiresAt > CURRENT_TIMESTAMP
        """)
    boolean hasActiveToken(UUID tenantId);

    // Eski tokenları temizle (tenant bazlı)
    @Modifying
    @Transactional
    @Query("DELETE FROM EmailVerificationToken t WHERE t.tenantId = :tenantId")
    void deleteAllByTenantId(UUID tenantId);
}
