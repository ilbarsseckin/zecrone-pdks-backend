package com.pdks.user;

import com.pdks.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    public List<User> findAll() {
        return userRepo.findAll();
    }

    public User findById(UUID id) {
        return userRepo.findById(id)
                .orElseThrow(() -> new BusinessException("Kullanıcı bulunamadı: " + id));
    }

    @Transactional
    public User create(UserDto dto) {
        if (userRepo.existsByEmail(dto.getEmail()))
            throw new BusinessException("Bu email zaten kayıtlı: " + dto.getEmail());

        User user = new User();
        user.setEmail(dto.getEmail());
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setRole(dto.getRole());
        user.setBranchId(dto.getBranchId());
        user.setEmployeeId(dto.getEmployeeId());

        return userRepo.save(user);
    }

    @Transactional
    public User update(UUID id, UserDto dto) {
        User user = findById(id);
        user.setEmail(dto.getEmail());
        user.setRole(dto.getRole());
        user.setBranchId(dto.getBranchId());
        user.setEmployeeId(dto.getEmployeeId());

        if (dto.getPassword() != null && !dto.getPassword().isBlank())
            user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));

        return userRepo.save(user);
    }

    @Transactional
    public void setActive(UUID id, boolean active) {
        User user = findById(id);
        user.setIsActive(active);
        userRepo.save(user);
    }

    @Transactional
    public void resetPassword(UUID id, String newPassword) {
        User user = findById(id);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepo.save(user);
    }
}