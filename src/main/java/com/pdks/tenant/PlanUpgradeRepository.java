package com.pdks.tenant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlanUpgradeRepository extends JpaRepository<PlanUpgradeRequest, UUID> {

    List<PlanUpgradeRequest> findAllByStatus(PlanUpgradeRequest.Status status);

    List<PlanUpgradeRequest> findAllByTenantId(UUID tenantId);

    /** Aynı tenant'ın zaten bekleyen talebi var mı? */
    Optional<PlanUpgradeRequest> findByTenantIdAndStatus(
        UUID tenantId, PlanUpgradeRequest.Status status);
}
