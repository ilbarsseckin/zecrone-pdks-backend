package com.pdks.overtime;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "overtimes")
@Data
public class Overtime {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID employeeId;

    @Column(nullable = false)
    private UUID branchId;

    @Column(nullable = false)
    private LocalDate workDate;

    private LocalTime startTime;
    private LocalTime endTime;

    // Dakika cinsinden fazla mesai süresi
    private Integer overtimeMinutes = 0;

    @Enumerated(EnumType.STRING)
    private OvertimeType type = OvertimeType.WEEKDAY;

    @Enumerated(EnumType.STRING)
    private OvertimeStatus status = OvertimeStatus.PENDING;

    private String description;
    private UUID approvedBy;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum OvertimeType {
        WEEKDAY,   // Hafta içi
        WEEKEND,   // Hafta sonu
        HOLIDAY,   // Resmi tatil
        NIGHT      // Gece mesaisi
    }

    public enum OvertimeStatus {
        PENDING,   // Onay bekliyor
        APPROVED,  // Onaylandı
        REJECTED   // Reddedildi
    }
}