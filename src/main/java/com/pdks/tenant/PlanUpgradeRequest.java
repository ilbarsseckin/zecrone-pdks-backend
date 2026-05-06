package com.pdks.tenant;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Bir tenant'ın plan yükseltme talebi.
 * SUPER_ADMIN onaylarsa changePlan() çağrılır.
 * İleride: ödeme webhook'u onayladığında otomatik onaylanır.
 */
@Entity
@Table(name = "plan_upgrade_requests", schema = "public")
@Data
public class PlanUpgradeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Tenant.Plan currentPlan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Tenant.Plan requestedPlan;

    private String note;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    /** SUPER_ADMIN onaylayan/reddeden */
    private UUID reviewedBy;

    private String reviewNote;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum Status {
        PENDING,   // Bekliyor
        APPROVED,  // Onaylandı
        REJECTED   // Reddedildi
    }
}
