package com.pdks.attendance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttendanceRepository extends JpaRepository<AttendanceRecord, UUID> {

    Optional<AttendanceRecord> findByEmployeeIdAndWorkDate(UUID employeeId, LocalDate date);

    List<AttendanceRecord> findAllByBranchIdAndWorkDate(UUID branchId, LocalDate date);

    List<AttendanceRecord> findAllByEmployeeIdAndWorkDateBetween(
        UUID employeeId, LocalDate from, LocalDate to);

    List<AttendanceRecord> findAllByBranchIdAndWorkDateBetween(
        UUID branchId, LocalDate from, LocalDate to);

    @Query("SELECT COUNT(a) FROM AttendanceRecord a " +
           "WHERE a.employeeId = :employeeId " +
           "AND a.workDate BETWEEN :from AND :to " +
           "AND a.status = 'PRESENT'")
    long countPresentDays(UUID employeeId, LocalDate from, LocalDate to);
}
