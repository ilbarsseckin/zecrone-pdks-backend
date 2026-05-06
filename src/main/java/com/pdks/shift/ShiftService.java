package com.pdks.shift;

import com.pdks.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShiftService {

    private final ShiftRepository shiftRepo;

    public List<Shift> findByBranchAndDate(UUID branchId, LocalDate date) {
        return shiftRepo.findAllByBranchIdAndShiftDate(branchId, date);
    }

    public List<Shift> findByBranchAndRange(UUID branchId, LocalDate from, LocalDate to) {
        return shiftRepo.findAllByBranchIdAndShiftDateBetween(branchId, from, to);
    }

    public List<Shift> findByEmployee(UUID employeeId) {
        return shiftRepo.findAllByEmployeeId(employeeId);
    }

    @Transactional
    public Shift create(ShiftDto dto) {
        // Çakışma kontrolü — aynı personel aynı gün
        List<Shift> existing = shiftRepo
                .findAllByBranchIdAndShiftDate(dto.getBranchId(), dto.getShiftDate());

        boolean conflict = existing.stream()
                .anyMatch(s -> s.getEmployeeId().equals(dto.getEmployeeId()));

        if (conflict)
            throw new BusinessException(
                    "Bu personelin " + dto.getShiftDate() +
                            " tarihinde zaten bir vardiyası var");

        // Saat çakışması kontrolü
        boolean timeConflict = existing.stream()
                .anyMatch(s -> s.getEmployeeId().equals(dto.getEmployeeId()) &&
                        isTimeOverlap(s.getStartTime(), s.getEndTime(),
                                dto.getStartTime(), dto.getEndTime()));

        if (timeConflict)
            throw new BusinessException(
                    "Vardiya saatleri mevcut bir vardiyayla çakışıyor");

        Shift shift = new Shift();
        shift.setBranchId(dto.getBranchId());
        shift.setEmployeeId(dto.getEmployeeId());
        shift.setName(dto.getName());
        shift.setShiftDate(dto.getShiftDate());
        shift.setStartTime(dto.getStartTime());
        shift.setEndTime(dto.getEndTime());
        shift.setType(dto.getType());
        return shiftRepo.save(shift);
    }

    private boolean isTimeOverlap(
            LocalTime start1, LocalTime end1,
            LocalTime start2, LocalTime end2) {
        return start1.isBefore(end2) && start2.isBefore(end1);
    }

    @Transactional
    public void delete(UUID id) {
        shiftRepo.deleteById(id);
    }
}