package com.pdks.shift;

import com.pdks.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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

    @Transactional
    public void delete(UUID id) {
        shiftRepo.deleteById(id);
    }
}