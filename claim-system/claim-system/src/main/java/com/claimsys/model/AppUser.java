package com.claimsys.model;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class AppUser {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    @Column(unique = true, nullable = false)
    private String email;
    @Column(nullable = false)
    private String password; // BCrypt hash
    private String policyNumber;
    @Enumerated(EnumType.STRING)
    private Role role;

    public AppUser() {}
    public AppUser(String name, String email, String password, String policyNumber, Role role) {
        this.name = name; this.email = email; this.password = password;
        this.policyNumber = policyNumber; this.role = role;
    }
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getPolicyNumber() { return policyNumber; }
    public Role getRole() { return role; }
}
