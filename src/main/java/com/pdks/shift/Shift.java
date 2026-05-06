package com.pdks.shift;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "shifts")
@Data
public class Shift {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID branchId;
    private UUID employeeId;

    private String name;

    private LocalDate shiftDate;
    private LocalTime startTime;
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    private ShiftType type = ShiftType.MORNING;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum ShiftType {
        MORNING,   // Sabah
        AFTERNOON, // Öğleden sonra
        NIGHT,     // Gece
        CUSTOM     // Özel
    }
}