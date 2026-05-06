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
        STARTER, PROFESSIONAL, ENTERPRISE;

        public boolean canExportExcel() {
            return this == PROFESSIONAL || this == ENTERPRISE;
        }

        public boolean canUseQr() {
            return this == PROFESSIONAL || this == ENTERPRISE;
        }

        public boolean canUseApi() {
            return this == ENTERPRISE;
        }

        public boolean canUseErp() {
            return this == ENTERPRISE;
        }

        public int getMaxBranches() {
            return switch (this) {
                case STARTER      -> 1;
                case PROFESSIONAL -> 20;
                case ENTERPRISE   -> Integer.MAX_VALUE;
            };
        }

        public int getMaxEmployees() {
            return switch (this) {
                case STARTER      -> 50;
                case PROFESSIONAL -> 500;
                case ENTERPRISE   -> Integer.MAX_VALUE;
            };
        }
    }
}
