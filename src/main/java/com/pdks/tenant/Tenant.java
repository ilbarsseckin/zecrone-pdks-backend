package com.pdks.tenant;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tenants", schema = "public")
@Data
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String companyName;

    @Column(unique = true, nullable = false)
    private String schemaName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Plan plan = Plan.STARTER;

    private Integer maxBranches  = 3;
    private Integer maxEmployees = 50;

    private String contactEmail;
    private String contactPhone;

    private Boolean isActive = true;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum Plan {
        STARTER, PROFESSIONAL, ENTERPRISE
    }
}
