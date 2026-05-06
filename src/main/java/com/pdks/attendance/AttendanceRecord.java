package com.pdks.attendance;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "attendance_records",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"employee_id", "work_date"}))
@Data
public class AttendanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(nullable = false)
    private LocalDate workDate;

    private LocalDateTime checkIn;
    private LocalDateTime checkOut;

    // Dakika cinsinden çalışma süresi
    private Integer workMinutes = 0;

    @Enumerated(EnumType.STRING)
    private Status status = Status.ABSENT;

    private String notes;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum Status {
        PRESENT, LATE, ABSENT, HALF_DAY, ON_LEAVE
    }
}
