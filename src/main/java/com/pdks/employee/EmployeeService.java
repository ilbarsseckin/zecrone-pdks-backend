package com.pdks.employee;

import com.pdks.attendance.AttendanceRecord;
import com.pdks.common.BusinessException;
import com.pdks.config.TenantContext;
import com.pdks.tenant.Tenant;
import com.pdks.tenant.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepo;
    private final TenantRepository   tenantRepo;
    private final com.pdks.branch.BranchRepository branchRepo;
    private final com.pdks.attendance.AttendanceRepository attendanceRepo;
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
// Otomatik QR token üret
        emp.setQrToken(java.util.UUID.randomUUID().toString().replace("-", "").toUpperCase());
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

    @Transactional
    public Map<String, Object> checkinByToken(String token, UUID branchId) {
        // QR token veya RF kart ID ile personel bul
        Employee emp = employeeRepo.findByQrToken(token)
                .or(() -> employeeRepo.findByRfCardId(token))
                .orElseThrow(() -> new BusinessException("Kart veya QR kod tanınmadı"));

        LocalDate today = LocalDate.now();
        var existing = attendanceRepo.findByEmployeeIdAndWorkDate(emp.getId(), today);

        if (existing.isEmpty()) {
            // Giriş yap
            AttendanceRecord record = new AttendanceRecord();
            record.setEmployeeId(emp.getId());
            record.setBranchId(branchId);
            record.setWorkDate(today);
            record.setCheckIn(java.time.LocalDateTime.now());

            com.pdks.branch.Branch branch = branchRepo.findById(branchId).orElse(null);
            if (branch != null) {
                java.time.LocalTime now = java.time.LocalTime.now();
                java.time.LocalTime limit = branch.getWorkStart().plusMinutes(branch.getLateTolerance());
                record.setStatus(now.isAfter(limit) ?
                        AttendanceRecord.Status.LATE : AttendanceRecord.Status.PRESENT);
            }
            attendanceRepo.save(record);

            return Map.of(
                    "action",     "CHECK_IN",
                    "employeeId", emp.getId(),
                    "fullName",   emp.getFullName(),
                    "status",     record.getStatus(),
                    "time",       record.getCheckIn().toString()
            );
        } else {
            // Çıkış yap
            AttendanceRecord record = existing.get();
            if (record.getCheckOut() != null)
                throw new BusinessException("Bugün zaten çıkış yapıldı");

            record.setCheckOut(java.time.LocalDateTime.now());
            long minutes = java.time.temporal.ChronoUnit.MINUTES.between(
                    record.getCheckIn(), record.getCheckOut());
            record.setWorkMinutes((int) minutes);
            attendanceRepo.save(record);

            return Map.of(
                    "action",      "CHECK_OUT",
                    "employeeId",  emp.getId(),
                    "fullName",    emp.getFullName(),
                    "workMinutes", record.getWorkMinutes(),
                    "time",        record.getCheckOut().toString()
            );
        }
    }

    @Transactional
    public Employee setRfCard(UUID id, String cardId) {
        Employee emp = findById(id);
        emp.setRfCardId(cardId);
        return employeeRepo.save(emp);
    }

    @Transactional
    public Employee regenerateQr(UUID id) {
        Employee emp = findById(id);
        emp.setQrToken(java.util.UUID.randomUUID().toString().replace("-", "").toUpperCase());
        return employeeRepo.save(emp);
    }
}
