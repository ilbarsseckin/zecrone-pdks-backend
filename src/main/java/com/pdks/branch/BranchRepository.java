package com.pdks.branch;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BranchRepository extends JpaRepository<Branch, UUID> {

    List<Branch> findAllByIsActiveTrue();

    boolean existsByName(String name);

    /**
     * Aktif şube sayısını döner.
     * Multi-tenant mimaride her tenant kendi schema'sında çalıştığı için
     * ayrıca tenantId filtresi gerekmez — sayım otomatik olarak izole.
     */
    long countByIsActiveTrue();
}
