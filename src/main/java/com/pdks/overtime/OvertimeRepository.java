package com.pdks.overtime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface OvertimeRepository extends JpaRepository<Overtime, UUID> {
    List<Overtime> findAllByEmployeeId(UUID employeeId);
    List<Overtime> findAllByBranchIdAndWorkDateBetween(UUID branchId, LocalDate from, LocalDate to);
    List<Overtime> findAllByStatus(Overtime.OvertimeStatus status);
}