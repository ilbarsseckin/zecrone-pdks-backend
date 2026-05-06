package com.pdks.attendance;

import com.pdks.branch.Branch;
import com.pdks.branch.BranchRepository;
import com.pdks.common.BusinessException;
import com.pdks.employee.Employee;
import com.pdks.employee.EmployeeRepository;
import com.pdks.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttendanceService {

    private final AttendanceRepository  attendanceRepo;
    private final EmployeeRepository    employeeRepo;
    private final BranchRepository      branchRepo;
    private final NotificationService   notificationService;

    @Transactional
    public AttendanceRecord checkIn(UUID employeeId) {
        LocalDate today = LocalDate.now();

        if (attendanceRepo.findByEmployeeIdAndWorkDate(employeeId, today).isPresent())
            throw new BusinessException("Bugün zaten giriş yapıldı");

        Employee emp = employeeRepo.findById(employeeId)
            .orElseThrow(() -> new BusinessException("Personel bulunamadı"));

        Branch branch = branchRepo.findById(emp.getBranchId())
            .orElseThrow(() -> new BusinessException("Şube bulunamadı"));

        AttendanceRecord record = new AttendanceRecord();
        record.setEmployeeId(employeeId);
        record.setBranchId(emp.getBranchId());
        record.setWorkDate(today);
        record.setCheckIn(LocalDateTime.now());

        LocalTime now   = LocalTime.now();
        LocalTime limit = branch.getWorkStart().plusMinutes(branch.getLateTolerance());
        record.setStatus(now.isAfter(limit)
            ? AttendanceRecord.Status.LATE
            : AttendanceRecord.Status.PRESENT);

        record = attendanceRepo.save(record);

        // Geç geldiyse çalışana bildirim gönder
        if (record.getStatus() == AttendanceRecord.Status.LATE && emp.getPushToken() != null) {
         //   notificationService.sendToDevice(emp.getPushToken(), "⏰ Geç Giriş Kaydedildi", "Bugün " + branch.getWorkStart() + " yerine " + now.withSecond(0).withNano(0) + " saatinde giriş yaptınız.", java.util.Map.of("type", "LATE_CHECKIN"));
        }

        log.info("Giriş: {} - {}", emp.getFullName(), record.getStatus());
        return record;
    }

    @Transactional
    public AttendanceRecord checkOut(UUID employeeId) {
        LocalDate today = LocalDate.now();

        AttendanceRecord record = attendanceRepo
            .findByEmployeeIdAndWorkDate(employeeId, today)
            .orElseThrow(() -> new BusinessException(
                "Bugün giriş kaydı yok. Önce giriş yapın."));

        if (record.getCheckOut() != null)
            throw new BusinessException("Bugün zaten çıkış yapıldı");

        record.setCheckOut(LocalDateTime.now());
        long minutes = ChronoUnit.MINUTES.between(
            record.getCheckIn(), record.getCheckOut());
        record.setWorkMinutes((int) minutes);

        record = attendanceRepo.save(record);

        // Çalışana günlük özet bildirimi
        Employee emp = employeeRepo.findById(employeeId).orElse(null);
        if (emp != null && emp.getPushToken() != null) {
            long hours = minutes / 60;
            long mins  = minutes % 60;
          //  notificationService.sendToDevice(emp.getPushToken(), "👋 Çıkış Kaydedildi", "Bugün " + hours + " saat " + mins + " dakika çalıştınız.", java.util.Map.of("type", "CHECKOUT", "minutes", String.valueOf(minutes)));
        }

        log.info("Çıkış: {} - {} dakika", employeeId, minutes);
        return record;
    }

    public List<AttendanceRecord> getDailyReport(UUID branchId, LocalDate date) {
        return attendanceRepo.findAllByBranchIdAndWorkDate(branchId, date);
    }

    public List<AttendanceRecord> getMonthlyReport(UUID employeeId, int year, int month) {
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to   = from.withDayOfMonth(from.lengthOfMonth());
        return attendanceRepo.findAllByEmployeeIdAndWorkDateBetween(employeeId, from, to);
    }

    public AttendanceRecord getTodayRecord(UUID employeeId) {
        return attendanceRepo
            .findByEmployeeIdAndWorkDate(employeeId, LocalDate.now())
            .orElse(null);
    }
}
