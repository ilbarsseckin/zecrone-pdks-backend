package com.pdks.leave;

import com.pdks.common.BusinessException;
import com.pdks.employee.Employee;
import com.pdks.employee.EmployeeRepository;
import com.pdks.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LeaveService {

    private final LeaveRepository        leaveRepo;
    private final LeaveBalanceRepository balanceRepo;
    private final EmployeeRepository     employeeRepo;
    private final NotificationService    notificationService;

    public List<Leave> findAll() {
        return leaveRepo.findAll();
    }

    public List<Leave> findByEmployee(UUID employeeId) {
        return leaveRepo.findAllByEmployeeId(employeeId);
    }

    public List<Leave> findPending() {
        return leaveRepo.findAllByStatus(Leave.LeaveStatus.PENDING);
    }

    /** Çalışanın belirli yıl için izin bakiyesi — yoksa otomatik oluşturur */
    public LeaveBalance getOrCreateBalance(UUID employeeId, int year) {
        return balanceRepo.findByEmployeeIdAndYear(employeeId, year)
            .orElseGet(() -> {
                LeaveBalance bal = new LeaveBalance();
                bal.setEmployeeId(employeeId);
                bal.setYear(year);
                bal.setEntitledDays(14); // Varsayılan yıllık izin
                return balanceRepo.save(bal);
            });
    }

    /** Bakiye güncelle */
    @Transactional
    public void setEntitledDays(UUID employeeId, int year, int days) {
        LeaveBalance bal = getOrCreateBalance(employeeId, year);
        bal.setEntitledDays(days);
        balanceRepo.save(bal);
    }

    @Transactional
    public Leave create(LeaveDto dto) {
        int year = dto.getStartDate().getYear();
        int totalDays = (int) ChronoUnit.DAYS.between(
            dto.getStartDate(), dto.getEndDate()) + 1;

        // Yıllık izin için bakiye kontrolü
        if (dto.getType() == Leave.LeaveType.ANNUAL) {
            LeaveBalance bal = getOrCreateBalance(dto.getEmployeeId(), year);
            if (bal.getRemainingDays() < totalDays) {
                throw new BusinessException(
                    "Yeterli izin hakkınız yok. Kalan: " +
                    bal.getRemainingDays() + " gün, Talep: " + totalDays + " gün.");
            }
            // Bekleyen güne ekle
            bal.setPendingDays(bal.getPendingDays() + totalDays);
            balanceRepo.save(bal);
        }

        Leave leave = new Leave();
        leave.setEmployeeId(dto.getEmployeeId());
        leave.setType(dto.getType());
        leave.setStartDate(dto.getStartDate());
        leave.setEndDate(dto.getEndDate());
        leave.setDescription(dto.getDescription());
        leave.setTotalDays(totalDays);
        leave.setRequestedBy(dto.getRequestedBy() != null ? dto.getRequestedBy() : "WEB");
        leave = leaveRepo.save(leave);

        // Çalışana bildirim
        Employee emp = employeeRepo.findById(dto.getEmployeeId()).orElse(null);
        if (emp != null && emp.getPushToken() != null) {
            String dates = dto.getStartDate() + " – " + dto.getEndDate();
            //notificationService.sendToDevice(emp.getPushToken(), "📋 İzin Talebiniz Alındı", dates + " için izin talebiniz onay bekliyor.", java.util.Map.of("type", "LEAVE_CREATED", "leaveId", leave.getId().toString()));
        }

        return leave;
    }

    @Transactional
    public Leave approve(UUID id, UUID reviewerId, String reviewNote) {
        Leave leave = leaveRepo.findById(id)
            .orElseThrow(() -> new BusinessException("İzin bulunamadı"));

        if (leave.getStatus() != Leave.LeaveStatus.PENDING)
            throw new BusinessException("Bu izin zaten işleme alınmış");

        leave.setStatus(Leave.LeaveStatus.APPROVED);
        leave.setReviewedBy(reviewerId);
        leave.setReviewNote(reviewNote);
        leave.setReviewedAt(LocalDateTime.now());
        leave = leaveRepo.save(leave);

        // Bakiyeyi güncelle: pending → used
        if (leave.getType() == Leave.LeaveType.ANNUAL) {
            LeaveBalance bal = getOrCreateBalance(
                leave.getEmployeeId(), leave.getStartDate().getYear());
            bal.setPendingDays(Math.max(0, bal.getPendingDays() - leave.getTotalDays()));
            bal.setUsedDays(bal.getUsedDays() + leave.getTotalDays());
            balanceRepo.save(bal);
        }

        // Çalışana bildirim
        Employee emp = employeeRepo.findById(leave.getEmployeeId()).orElse(null);
        if (emp != null && emp.getPushToken() != null) {
         //   notificationService.notifyLeaveApproved(emp.getPushToken(), emp.getFullName(), leave.getStartDate() + " – " + leave.getEndDate());
        }

        return leave;
    }

    @Transactional
    public Leave reject(UUID id, UUID reviewerId, String reviewNote) {
        Leave leave = leaveRepo.findById(id)
            .orElseThrow(() -> new BusinessException("İzin bulunamadı"));

        if (leave.getStatus() != Leave.LeaveStatus.PENDING)
            throw new BusinessException("Bu izin zaten işleme alınmış");

        leave.setStatus(Leave.LeaveStatus.REJECTED);
        leave.setReviewedBy(reviewerId);
        leave.setReviewNote(reviewNote);
        leave.setReviewedAt(LocalDateTime.now());
        leave = leaveRepo.save(leave);

        // Bakiyeyi güncelle: pending geri al
        if (leave.getType() == Leave.LeaveType.ANNUAL) {
            LeaveBalance bal = getOrCreateBalance(
                leave.getEmployeeId(), leave.getStartDate().getYear());
            bal.setPendingDays(Math.max(0, bal.getPendingDays() - leave.getTotalDays()));
            balanceRepo.save(bal);
        }

        // Çalışana bildirim
        Employee emp = employeeRepo.findById(leave.getEmployeeId()).orElse(null);
        if (emp != null && emp.getPushToken() != null) {
          //  notificationService.notifyLeaveRejected(emp.getPushToken(), emp.getFullName(), leave.getStartDate() + " – " + leave.getEndDate());
        }

        return leave;
    }
}
