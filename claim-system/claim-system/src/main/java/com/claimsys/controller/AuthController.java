package com.claimsys.controller;

import com.claimsys.dto.Dtos.*;
import com.claimsys.model.AppUser;
import com.claimsys.model.Role;
import com.claimsys.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository users;
    private final PasswordEncoder encoder;

    public AuthController(UserRepository users, PasswordEncoder encoder) {
        this.users = users; this.encoder = encoder;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest req) {
        String email = req.email().toLowerCase();
        if (users.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        // Public registration always creates a CUSTOMER; employees are created by an admin/seed only.
        AppUser u = users.save(new AppUser(req.name(), email, encoder.encode(req.password()), req.policyNumber(), Role.CUSTOMER));
        return UserResponse.of(u);
    }

    @GetMapping("/me")
    public UserResponse me(Authentication auth) {
        return UserResponse.of(users.findByEmail(auth.getName()).orElseThrow());
    }
}
