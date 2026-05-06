package com.pdks.branch;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/branches")
@RequiredArgsConstructor
public class BranchController {

    private final BranchService branchService;

    // GET /api/branches
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    public ResponseEntity<List<Branch>> list() {
        return ResponseEntity.ok(branchService.findAll());
    }

    // GET /api/branches/{id}
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<Branch> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(branchService.findById(id));
    }

    // POST /api/branches
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Branch> create(
            @Valid @RequestBody BranchDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(branchService.create(dto));
    }

    // PUT /api/branches/{id}
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Branch> update(
            @PathVariable UUID id,
            @Valid @RequestBody BranchDto dto) {
        return ResponseEntity.ok(branchService.update(id, dto));
    }

    // DELETE /api/branches/{id}
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        branchService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
