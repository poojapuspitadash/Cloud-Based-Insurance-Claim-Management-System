package com.claimsys.dto;

import com.claimsys.model.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class Dtos {
    private Dtos() {}

    public record RegisterRequest(
            @NotBlank String name,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, message = "password must be at least 8 characters") String password,
            @NotBlank String policyNumber) {}

    public record CreateClaimRequest(
            @NotBlank String type,
            @NotBlank @Size(max = 2000) String description,
            @NotNull @DecimalMin(value = "1.0", message = "amount must be positive") BigDecimal amount,
            @NotNull @PastOrPresent LocalDate incidentDate) {}

    public record StatusUpdateRequest(@NotNull ClaimStatus status, @Size(max = 500) String note) {}

    public record UserResponse(Long id, String name, String email, String policyNumber, Role role) {
        public static UserResponse of(AppUser u) {
            return new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getPolicyNumber(), u.getRole());
        }
    }

    public record DocumentResponse(Long id, String fileName, String contentType, LocalDateTime uploadedAt) {
        public static DocumentResponse of(ClaimDocument d) {
            return new DocumentResponse(d.getId(), d.getFileName(), d.getContentType(), d.getUploadedAt());
        }
    }

    public record ClaimResponse(Long id, String claimNumber, String customerName, String policyNumber,
                                String type, String description, BigDecimal amount, LocalDate incidentDate,
                                ClaimStatus status, Priority priority, Integer riskScore, String reviewerNote,
                                LocalDateTime createdAt, LocalDateTime updatedAt, List<DocumentResponse> documents) {
        public static ClaimResponse of(Claim c, List<ClaimDocument> docs) {
            return new ClaimResponse(c.getId(), c.getClaimNumber(), c.getCustomer().getName(),
                    c.getCustomer().getPolicyNumber(), c.getType(), c.getDescription(), c.getAmount(),
                    c.getIncidentDate(), c.getStatus(), c.getPriority(), c.getRiskScore(), c.getReviewerNote(),
                    c.getCreatedAt(), c.getUpdatedAt(), docs.stream().map(DocumentResponse::of).toList());
        }
    }
}
