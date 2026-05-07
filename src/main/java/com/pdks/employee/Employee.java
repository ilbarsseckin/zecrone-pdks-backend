package com.pdks.employee;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "employees")
@Data
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID branchId;

    private UUID userId;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    private String email;
    private String phone;
    private String department;
    private String position;

    @Enumerated(EnumType.STRING)
    private Status status = Status.ACTIVE;

    private LocalDate startDate;
    private LocalDate endDate;

    // ── Mobil auth alanları ──────────────────────────────────────────────────

    /** BCrypt hash — mobil uygulamaya giriş için */
    private String passwordHash;

    /** Firebase / APNs push token — bildirim göndermek için */
    private String pushToken;

    private LocalDateTime lastLogin;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum Status {
        ACTIVE, PASSIVE, ON_LEAVE
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    @Column(unique = true)
    private String employeeNumber;

    @Column(unique = true)
    private String qrToken;

    @Column(unique = true)
    private String rfCardId;

}
