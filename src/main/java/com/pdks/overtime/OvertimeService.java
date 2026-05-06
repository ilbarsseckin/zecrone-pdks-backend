package com.pdks.overtime;

import com.pdks.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OvertimeService {

    private final OvertimeRepository overtimeRepo;

    public List<Overtime> findByEmployee(UUID employeeId) {
        return overtimeRepo.findAllByEmployeeId(employeeId);
    }

    public List<Overtime> findByBranchAndRange(UUID branchId, LocalDate from, LocalDate to) {
        return overtimeRepo.findAllByBranchIdAndWorkDateBetween(branchId, from, to);
    }

    public List<Overtime> findPending() {
        return overtimeRepo.findAllByStatus(Overtime.OvertimeStatus.PENDING);
    }

    @Transactional
    public Overtime create(OvertimeDto dto) {
        int minutes = (int) ChronoUnit.MINUTES.between(dto.getStartTime(), dto.getEndTime());;

        if (minutes <= 0)
            throw new BusinessException("Bitiş saati başlangıç saatinden sonra olmalı");

        Overtime ot = new Overtime();
        ot.setEmployeeId(dto.getEmployeeId());
        ot.setBranchId(dto.getBranchId());
        ot.setWorkDate(dto.getWorkDate());
        ot.setStartTime(dto.getStartTime());
        ot.setEndTime(dto.getEndTime());
        ot.setOvertimeMinutes(minutes);
        ot.setType(dto.getType());
        ot.setDescription(dto.getDescription());
        return overtimeRepo.save(ot);
    }

    @Transactional
    public Overtime approve(UUID id, UUID reviewerId) {
        Overtime ot = overtimeRepo.findById(id)
                .orElseThrow(() -> new BusinessException("Mesai kaydı bulunamadı"));
        if (ot.getStatus() != Overtime.OvertimeStatus.PENDING)
            throw new BusinessException("Bu mesai zaten işleme alınmış");
        ot.setStatus(Overtime.OvertimeStatus.APPROVED);
        ot.setApprovedBy(reviewerId);
        return overtimeRepo.save(ot);
    }

    @Transactional
    public Overtime reject(UUID id) {
        Overtime ot = overtimeRepo.findById(id)
                .orElseThrow(() -> new BusinessException("Mesai kaydı bulunamadı"));
        ot.setStatus(Overtime.OvertimeStatus.REJECTED);
        return overtimeRepo.save(ot);
    }
}