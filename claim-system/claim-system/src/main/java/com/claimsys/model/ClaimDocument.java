package com.claimsys.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "claim_documents")
public class ClaimDocument {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    private Claim claim;
    private String fileName;
    private String contentType;
    private String storageKey; // S3 object key (or local path in dev)
    private LocalDateTime uploadedAt = LocalDateTime.now();

    public ClaimDocument() {}
    public ClaimDocument(Claim claim, String fileName, String contentType, String storageKey) {
        this.claim = claim; this.fileName = fileName; this.contentType = contentType; this.storageKey = storageKey;
    }
    public Long getId() { return id; }
    public Claim getClaim() { return claim; }
    public String getFileName() { return fileName; }
    public String getContentType() { return contentType; }
    public String getStorageKey() { return storageKey; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
}
