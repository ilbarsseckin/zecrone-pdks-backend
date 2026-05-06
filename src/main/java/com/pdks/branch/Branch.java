package com.pdks.branch;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "branches")
@Data
public class Branch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String city;
    private String address;
    private String phone;

    // Mesai saatleri
    private LocalTime workStart = LocalTime.of(8, 0);
    private LocalTime workEnd   = LocalTime.of(17, 0);

    // Geç kalma toleransı (dakika)
    private Integer lateTolerance = 5;

    private String timezone = "Europe/Istanbul";

    private Boolean isActive = true;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
