package com.pdks.leave;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface LeaveRepository extends JpaRepository<Leave, UUID> {

    List<Leave> findAllByEmployeeId(UUID employeeId);

    List<Leave> findAllByStatus(Leave.LeaveStatus status);

    List<Leave> findAllByStartDateGreaterThanEqualAndEndDateLessThanEqual(
        LocalDate from, LocalDate to);

    long countByEmployeeIdAndTypeAndStatus(
        UUID employeeId, Leave.LeaveType type, Leave.LeaveStatus status);
}
