package com.claimsys.controller;

import com.claimsys.dto.Dtos.*;
import com.claimsys.model.ClaimStatus;
import com.claimsys.repository.UserRepository;
import com.claimsys.service.ClaimService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Employee-only endpoints (secured by /api/employee/** -> ROLE_EMPLOYEE). */
@RestController
@RequestMapping("/api/employee/claims")
public class EmployeeController {
    private final ClaimService service;
    private final UserRepository users;

    public EmployeeController(ClaimService service, UserRepository users) {
        this.service = service; this.users = users;
    }

    @GetMapping
    public List<ClaimResponse> search(@RequestParam(required = false) ClaimStatus status,
                                      @RequestParam(required = false) String q) {
        return service.search(status, q);
    }

    @GetMapping("/stats")
    public Map<String, Long> stats() { return service.stats(); }

    @PatchMapping("/{id}/status")
    public ClaimResponse updateStatus(Authentication auth, @PathVariable Long id,
                                      @Valid @RequestBody StatusUpdateRequest req) {
        return service.updateStatus(users.findByEmail(auth.getName()).orElseThrow(), id, req);
    }
}
