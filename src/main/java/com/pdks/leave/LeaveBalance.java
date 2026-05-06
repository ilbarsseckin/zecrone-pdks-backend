package com.pdks.leave;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "leave_balances",
    uniqueConstraints = @UniqueConstraint(columnNames = {"employee_id", "year"}))
@Data
public class LeaveBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID employeeId;

    @Column(nullable = false)
    private Integer year;

    /** Yıl içinde hak edilen toplam izin günü */
    private Integer entitledDays = 14;

    /** Onaylanmış ve kullanılmış günler */
    private Integer usedDays = 0;

    /** Onay bekleyen izin günleri */
    private Integer pendingDays = 0;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /** Kullanılabilir kalan gün */
    @Transient
    public int getRemainingDays() {
        return entitledDays - usedDays - pendingDays;
    }
}
