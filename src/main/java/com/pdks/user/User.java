package com.pdks.user;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    private UserRole role = UserRole.STAFF;

    // null ise tüm şubeleri görür (ADMIN)
    private UUID branchId;

    // Bağlı personel
    private UUID employeeId;

    private Boolean isActive = true;
    private LocalDateTime lastLogin;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum UserRole {
        ADMIN, MANAGER, STAFF, SUPER_ADMIN
    }
}