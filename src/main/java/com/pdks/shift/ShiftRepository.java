package com.pdks.shift;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface ShiftRepository extends JpaRepository<Shift, UUID> {

    List<Shift> findAllByBranchIdAndShiftDate(UUID branchId, LocalDate shiftDate);

    List<Shift> findAllByBranchIdAndShiftDateBetween(UUID branchId, LocalDate from, LocalDate to);

    List<Shift> findAllByEmployeeId(UUID employeeId);
}