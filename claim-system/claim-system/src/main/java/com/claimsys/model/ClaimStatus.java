package com.claimsys.model;

/** Claim workflow: SUBMITTED -> UNDER_REVIEW -> APPROVED / REJECTED */
public enum ClaimStatus {
    SUBMITTED, UNDER_REVIEW, APPROVED, REJECTED;

    public boolean canMoveTo(ClaimStatus next) {
        return switch (this) {
            case SUBMITTED -> next == UNDER_REVIEW || next == REJECTED;
            case UNDER_REVIEW -> next == APPROVED || next == REJECTED;
            case APPROVED, REJECTED -> false; // final states
        };
    }
}
