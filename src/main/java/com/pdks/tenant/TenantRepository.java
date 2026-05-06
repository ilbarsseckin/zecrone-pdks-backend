package com.pdks.tenant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {
    Optional<Tenant> findByCompanyName(String companyName);
    Optional<Tenant> findByContactEmail(String email);
    boolean existsByCompanyName(String companyName);
    List<Tenant> findAllByIsActiveTrue();
}
