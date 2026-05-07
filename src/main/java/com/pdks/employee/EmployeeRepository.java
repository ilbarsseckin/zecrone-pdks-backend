package com.pdks.employee;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

    List<Employee> findAllByBranchId(UUID branchId);

    List<Employee> findAllByStatus(Employee.Status status);

    List<Employee> findAllByBranchIdAndStatus(UUID branchId, Employee.Status status);

    Optional<Employee> findByQrToken(String qrToken);
    Optional<Employee> findByRfCardId(String rfCardId);

    boolean existsByEmail(String email);

    long countByStatus(Employee.Status status);

    /** Mobil login için */
    Optional<Employee> findByEmailAndStatus(String email, Employee.Status status);
}
