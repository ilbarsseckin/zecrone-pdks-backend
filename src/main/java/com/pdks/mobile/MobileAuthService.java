package com.pdks.mobile;

import com.pdks.attendance.AttendanceRecord;
import com.pdks.attendance.AttendanceRepository;
import com.pdks.auth.JwtService;
import com.pdks.branch.Branch;
import com.pdks.branch.BranchRepository;
import com.pdks.common.BusinessException;
import com.pdks.employee.Employee;
import com.pdks.employee.EmployeeRepository;
import com.pdks.leave.LeaveBalance;
import com.pdks.leave.LeaveDto;
import com.pdks.leave.LeaveService;
import com.pdks.tenant.Tenant;
import com.pdks.tenant.TenantRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

// ─── Service ─────────────────────────────────────────────────────────────────

@Service
@RequiredArgsConstructor
@Slf4j
public class MobileAuthService {

    private final TenantRepository     tenantRepo;
    private final EmployeeRepository   employeeRepo;
    private final BranchRepository     branchRepo;
    private final AttendanceRepository attendanceRepo;
    private final JwtService           jwtService;
    private final PasswordEncoder      passwordEncoder;

    @Transactional
    public MobileAuthResponseDto login(MobileLoginDto dto) {
        Tenant tenant = tenantRepo.findByContactEmail(dto.tenantEmail)
            .orElseThrow(() -> new BusinessException("Firma bulunamadı"));

        if (!tenant.getIsActive())
            throw new BusinessException("Firma hesabı askıya alınmış");

        Employee emp = employeeRepo.findByEmailAndStatus(
                dto.employeeEmail, Employee.Status.ACTIVE)
            .orElseThrow(() -> new BusinessException("E-posta veya şifre hatalı"));

        if (emp.getPasswordHash() == null)
            throw new BusinessException(
                "Mobil şifreniz henüz belirlenmemiş. Lütfen yöneticinizle iletişime geçin.");

        if (!passwordEncoder.matches(dto.password, emp.getPasswordHash()))
            throw new BusinessException("E-posta veya şifre hatalı");

        if (dto.pushToken != null && !dto.pushToken.isBlank())
            emp.setPushToken(dto.pushToken);

        emp.setLastLogin(LocalDateTime.now());
        employeeRepo.save(emp);

        String token = jwtService.generateToken(
                emp.getId(),
                tenant.getId(),
                tenant.getSchemaName(),
                "STAFF",
                emp.getBranchId(),
                tenant.getPlan().name()
        );

        Branch branch = branchRepo.findById(emp.getBranchId()).orElse(null);

        MobileAuthResponseDto res = new MobileAuthResponseDto();
        res.token      = token;
        res.employeeId = emp.getId();
        res.fullName   = emp.getFullName();
        res.branchId   = emp.getBranchId();
        res.branchName = branch != null ? branch.getName() : null;

        log.info("Mobil giriş: {} @ {}", emp.getEmail(), tenant.getCompanyName());
        return res;
    }

    @Transactional
    public AttendanceRecord qrCheckIn(UUID employeeId, UUID branchId) {
        LocalDate today = LocalDate.now();

        if (attendanceRepo.findByEmployeeIdAndWorkDate(employeeId, today).isPresent())
            throw new BusinessException("Bugün zaten giriş yapıldı");

        Employee emp = employeeRepo.findById(employeeId)
            .orElseThrow(() -> new BusinessException("Çalışan bulunamadı"));

        if (!emp.getBranchId().equals(branchId))
            throw new BusinessException("Bu QR kodu kendi şubenize ait değil");

        Branch branch = branchRepo.findById(branchId)
            .orElseThrow(() -> new BusinessException("Şube bulunamadı"));

        AttendanceRecord record = new AttendanceRecord();
        record.setEmployeeId(employeeId);
        record.setBranchId(branchId);
        record.setWorkDate(today);
        record.setCheckIn(LocalDateTime.now());

        LocalTime now   = LocalTime.now();
        LocalTime limit = branch.getWorkStart().plusMinutes(branch.getLateTolerance());
        record.setStatus(now.isAfter(limit)
            ? AttendanceRecord.Status.LATE
            : AttendanceRecord.Status.PRESENT);

        log.info("QR Check-in: {} - {}", emp.getFullName(), record.getStatus());
        return attendanceRepo.save(record);
    }

    @Transactional
    public AttendanceRecord qrCheckOut(UUID employeeId) {
        LocalDate today = LocalDate.now();

        AttendanceRecord record = attendanceRepo
            .findByEmployeeIdAndWorkDate(employeeId, today)
            .orElseThrow(() -> new BusinessException("Bugün giriş kaydı bulunamadı."));

        if (record.getCheckOut() != null)
            throw new BusinessException("Bugün zaten çıkış yapıldı");

        record.setCheckOut(LocalDateTime.now());
        long minutes = ChronoUnit.MINUTES.between(record.getCheckIn(), record.getCheckOut());
        record.setWorkMinutes((int) minutes);
        return attendanceRepo.save(record);
    }

    @Transactional
    public void changePassword(UUID employeeId, MobileChangePasswordDto dto) {
        Employee emp = employeeRepo.findById(employeeId)
            .orElseThrow(() -> new BusinessException("Çalışan bulunamadı"));

        if (!passwordEncoder.matches(dto.currentPassword, emp.getPasswordHash()))
            throw new BusinessException("Mevcut şifre hatalı");

        emp.setPasswordHash(passwordEncoder.encode(dto.newPassword));
        employeeRepo.save(emp);
        log.info("Mobil şifre değiştirildi: {}", emp.getEmail());
    }

    @Transactional
    public void setInitialPassword(UUID employeeId, String rawPassword) {
        Employee emp = employeeRepo.findById(employeeId)
            .orElseThrow(() -> new BusinessException("Çalışan bulunamadı"));
        emp.setPasswordHash(passwordEncoder.encode(rawPassword));
        employeeRepo.save(emp);
        log.info("İlk mobil şifre belirlendi: {}", emp.getEmail());
    }

    public AttendanceRecord getTodayStatus(UUID employeeId) {
        return attendanceRepo
            .findByEmployeeIdAndWorkDate(employeeId, LocalDate.now())
            .orElse(null);
    }

    public List<AttendanceRecord> getMonthlyHistory(UUID employeeId, int year, int month) {
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to   = from.withDayOfMonth(from.lengthOfMonth());
        return attendanceRepo.findAllByEmployeeIdAndWorkDateBetween(employeeId, from, to);
    }
}

// ─── Controller ──────────────────────────────────────────────────────────────

@RestController
@RequestMapping("/api/mobile")
@RequiredArgsConstructor
class MobileController {

    private final MobileAuthService mobileAuthService;
    private final LeaveService      leaveService;

    @PostMapping("/auth/login")
    public ResponseEntity<MobileAuthResponseDto> login(
            @Valid @RequestBody MobileLoginDto dto) {
        return ResponseEntity.ok(mobileAuthService.login(dto));
    }

    @PostMapping("/attendance/check-in")
    public ResponseEntity<AttendanceRecord> checkIn(
            @AuthenticationPrincipal String employeeId,
            @Valid @RequestBody QrCheckInDto dto) {
        return ResponseEntity.ok(mobileAuthService.qrCheckIn(
            UUID.fromString(employeeId), UUID.fromString(dto.branchId)));
    }

    @PostMapping("/attendance/check-out")
    public ResponseEntity<AttendanceRecord> checkOut(
            @AuthenticationPrincipal String employeeId) {
        return ResponseEntity.ok(
            mobileAuthService.qrCheckOut(UUID.fromString(employeeId)));
    }

    @GetMapping("/attendance/today")
    public ResponseEntity<AttendanceRecord> today(
            @AuthenticationPrincipal String employeeId) {
        return ResponseEntity.ok(
            mobileAuthService.getTodayStatus(UUID.fromString(employeeId)));
    }

    @GetMapping("/attendance/history")
    public ResponseEntity<List<AttendanceRecord>> history(
            @AuthenticationPrincipal String employeeId,
            @RequestParam int year,
            @RequestParam int month) {
        return ResponseEntity.ok(
            mobileAuthService.getMonthlyHistory(UUID.fromString(employeeId), year, month));
    }

    @PostMapping("/auth/change-password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal String employeeId,
            @Valid @RequestBody MobileChangePasswordDto dto) {
        mobileAuthService.changePassword(UUID.fromString(employeeId), dto);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/leaves")
    public ResponseEntity<com.pdks.leave.Leave> requestLeave(
            @AuthenticationPrincipal String employeeId,
            @Valid @RequestBody LeaveDto dto) {
        dto.setEmployeeId(UUID.fromString(employeeId));
        dto.setRequestedBy("MOBILE");
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(leaveService.create(dto));
    }

    @GetMapping("/leaves")
    public ResponseEntity<List<com.pdks.leave.Leave>> myLeaves(
            @AuthenticationPrincipal String employeeId) {
        return ResponseEntity.ok(
            leaveService.findByEmployee(UUID.fromString(employeeId)));
    }

    @GetMapping("/leaves/balance")
    public ResponseEntity<LeaveBalance> myBalance(
            @AuthenticationPrincipal String employeeId,
            @RequestParam(defaultValue = "0") int year) {
        int y = year == 0 ? LocalDate.now().getYear() : year;
        return ResponseEntity.ok(
            leaveService.getOrCreateBalance(UUID.fromString(employeeId), y));
    }
}
