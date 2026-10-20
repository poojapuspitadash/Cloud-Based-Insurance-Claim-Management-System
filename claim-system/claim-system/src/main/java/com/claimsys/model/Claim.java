package com.claimsys.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "claims")
public class Claim {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true)
    private String claimNumber;
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    private AppUser customer;
    private String type;
    @Column(length = 2000)
    private String description;
    private BigDecimal amount;
    private LocalDate incidentDate;
    @Enumerated(EnumType.STRING)
    private ClaimStatus status = ClaimStatus.SUBMITTED;
    @Enumerated(EnumType.STRING)
    private Priority priority;
    private Integer riskScore;
    private String reviewerNote;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Long getId() { return id; }
    public String getClaimNumber() { return claimNumber; }
    public void setClaimNumber(String v) { this.claimNumber = v; }
    public AppUser getCustomer() { return customer; }
    public void setCustomer(AppUser v) { this.customer = v; }
    public String getType() { return type; }
    public void setType(String v) { this.type = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal v) { this.amount = v; }
    public LocalDate getIncidentDate() { return incidentDate; }
    public void setIncidentDate(LocalDate v) { this.incidentDate = v; }
    public ClaimStatus getStatus() { return status; }
    public void setStatus(ClaimStatus v) { this.status = v; this.updatedAt = LocalDateTime.now(); }
    public Priority getPriority() { return priority; }
    public void setPriority(Priority v) { this.priority = v; }
    public Integer getRiskScore() { return riskScore; }
    public void setRiskScore(Integer v) { this.riskScore = v; }
    public String getReviewerNote() { return reviewerNote; }
    public void setReviewerNote(String v) { this.reviewerNote = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
