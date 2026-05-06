package com.pdks.employee;

import com.pdks.common.BusinessException;
import com.pdks.config.TenantContext;
import com.pdks.tenant.Tenant;
import com.pdks.tenant.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepo;
    private final TenantRepository   tenantRepo;

    public List<Employee> findAll() {
        return employeeRepo.findAll();
    }
    public List<Employee> findByBranch(UUID branchId) {
        return employeeRepo.findAllByBranchIdAndStatus(
            branchId, Employee.Status.ACTIVE);
    }

    public Employee findById(UUID id) {
        return employeeRepo.findById(id)
            .orElseThrow(() -> new BusinessException("Personel bulunamadı: " + id));
    }

    @Transactional
    public Employee create(EmployeeDto dto) {
        Employee emp = new Employee();
        emp.setBranchId(dto.getBranchId());
        emp.setFirstName(dto.getFirstName());
        emp.setLastName(dto.getLastName());
        emp.setEmail(dto.getEmail());
        emp.setPhone(dto.getPhone());
        emp.setDepartment(dto.getDepartment());
        emp.setPosition(dto.getPosition());
        emp.setStartDate(dto.getStartDate());

        // Otomatik sicil numarası üret
        long count = employeeRepo.count() + 1;
        emp.setEmployeeNumber(String.format("EMP-%04d", count));

        return employeeRepo.save(emp);
    }

    @Transactional
    public Employee update(UUID id, EmployeeDto dto) {
        Employee emp = findById(id);
        emp.setBranchId(dto.getBranchId());
        emp.setFirstName(dto.getFirstName());
        emp.setLastName(dto.getLastName());
        emp.setEmail(dto.getEmail());
        emp.setPhone(dto.getPhone());
        emp.setDepartment(dto.getDepartment());
        emp.setPosition(dto.getPosition());
        return employeeRepo.save(emp);
    }

    @Transactional
    public void setStatus(UUID id, Employee.Status status) {
        Employee emp = findById(id);
        emp.setStatus(status);
        employeeRepo.save(emp);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    /**
     * Aktif tenant'ın personel limitini kontrol eder.
     * Sadece ACTIVE personel sayılır — pasife alınan veya izindekiler dahil değil.
     */
    private void checkEmployeeLimit() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) return; // Test ortamı veya SUPER_ADMIN isteği

        Tenant tenant = tenantRepo.findById(tenantId)
            .orElseThrow(() -> new BusinessException("Tenant bulunamadı"));

        long current = employeeRepo.countByStatus(Employee.Status.ACTIVE);

        if (current >= tenant.getMaxEmployees()) {
            throw new BusinessException(String.format(
                "Personel limitinize ulaştınız. Planınız en fazla %d aktif personele izin veriyor. " +
                "Daha fazla personel eklemek için planınızı yükseltin.",
                tenant.getMaxEmployees()
            ));
        }
    }


}
