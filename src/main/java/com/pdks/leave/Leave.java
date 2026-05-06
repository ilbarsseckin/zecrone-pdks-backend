package com.pdks.leave;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "leaves")
@Data
public class Leave {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID employeeId;

    @Enumerated(EnumType.STRING)
    private LeaveType type;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    private Integer totalDays;
    private String  description;

    @Enumerated(EnumType.STRING)
    private LeaveStatus status = LeaveStatus.PENDING;

    /** Onaylayan/reddeden kullanıcı */
    private UUID      reviewedBy;
    private String    reviewNote;
    private LocalDateTime reviewedAt;

    /** WEB veya MOBILE */
    private String requestedBy = "WEB";

    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum LeaveType {
        ANNUAL,       // Yıllık izin
        SICK,         // Hastalık
        MATERNITY,    // Doğum izni
        PATERNITY,    // Babalık izni
        MARRIAGE,     // Evlilik izni
        BEREAVEMENT,  // Vefat izni
        UNPAID,       // Ücretsiz izin
        OTHER
    }

    public enum LeaveStatus {
        PENDING,
        APPROVED,
        REJECTED
    }
}
