package com.claimsys.service;

import com.claimsys.dto.Dtos.*;
import com.claimsys.model.*;
import com.claimsys.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ClaimService {
    private static final Logger log = LoggerFactory.getLogger(ClaimService.class);
    private static final Set<String> ALLOWED_TYPES = Set.of("application/pdf", "image/jpeg", "image/png");

    private final ClaimRepository claims;
    private final ClaimDocumentRepository documents;
    private final FileStorageService storage;
    private final RiskScoringService risk;

    public ClaimService(ClaimRepository claims, ClaimDocumentRepository documents,
                        FileStorageService storage, RiskScoringService risk) {
        this.claims = claims; this.documents = documents; this.storage = storage; this.risk = risk;
    }

    @Transactional
    public ClaimResponse create(AppUser customer, CreateClaimRequest req) {
        Claim c = new Claim();
        c.setCustomer(customer);
        c.setType(req.type());
        c.setDescription(req.description());
        c.setAmount(req.amount());
        c.setIncidentDate(req.incidentDate());
        var r = risk.score(c);
        c.setRiskScore(r.score());
        c.setPriority(r.priority());
        c.setClaimNumber("TMP-" + UUID.randomUUID());
        c = claims.save(c);
        c.setClaimNumber(String.format("CL%05d", c.getId()));
        c = claims.save(c);
        log.info("Claim created claimNumber={} customer={} amount={} priority={}",
                c.getClaimNumber(), customer.getEmail(), c.getAmount(), c.getPriority());
        return toResponse(c);
    }

    public List<ClaimResponse> listForCustomer(AppUser customer) {
        return claims.findByCustomerOrderByCreatedAtDesc(customer).stream().map(this::toResponse).toList();
    }

    public List<ClaimResponse> search(ClaimStatus status, String q) {
        // No null parameters in the query (portable across H2/MySQL): empty text matches everything.
        String query = (q == null) ? "" : q.trim();
        List<ClaimStatus> statuses = (status == null) ? List.of(ClaimStatus.values()) : List.of(status);
        return claims.search(statuses, query).stream().map(this::toResponse).toList();
    }

    public ClaimResponse get(AppUser user, Long id) {
        return toResponse(loadAuthorized(user, id));
    }

    @Transactional
    public ClaimResponse updateStatus(AppUser reviewer, Long id, StatusUpdateRequest req) {
        Claim c = claims.findById(id).orElseThrow(() -> notFound("Claim not found"));
        if (!c.getStatus().canMoveTo(req.status())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot move claim from " + c.getStatus() + " to " + req.status());
        }
        c.setStatus(req.status());
        c.setReviewerNote(req.note());
        claims.save(c);
        log.info("Claim status changed claimNumber={} newStatus={} by={}", c.getClaimNumber(), req.status(), reviewer.getEmail());
        return toResponse(c);
    }

    @Transactional
    public DocumentResponse upload(AppUser user, Long claimId, MultipartFile file) {
        Claim c = loadAuthorized(user, claimId);
        if (file.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
        String type = file.getContentType();
        if (type == null || !ALLOWED_TYPES.contains(type)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only PDF, JPEG or PNG files are allowed");
        }
        String safeName = sanitize(file.getOriginalFilename());
        String key = "claims/" + c.getId() + "/" + UUID.randomUUID() + "-" + safeName;
        try {
            storage.store(key, file.getBytes(), type);
        } catch (IOException e) {
            log.error("Upload failed claimNumber={}", c.getClaimNumber(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not store file");
        }
        ClaimDocument d = documents.save(new ClaimDocument(c, safeName, type, key));
        log.info("Document uploaded claimNumber={} file={}", c.getClaimNumber(), safeName);
        return DocumentResponse.of(d);
    }

    public Map.Entry<ClaimDocument, byte[]> download(AppUser user, Long claimId, Long docId) {
        Claim c = loadAuthorized(user, claimId);
        ClaimDocument d = documents.findById(docId)
                .filter(x -> x.getClaim().getId().equals(c.getId()))
                .orElseThrow(() -> notFound("Document not found"));
        try {
            return Map.entry(d, storage.load(d.getStorageKey()));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not read file");
        }
    }

    public Map<String, Long> stats() {
        Map<String, Long> m = new java.util.LinkedHashMap<>();
        for (ClaimStatus s : ClaimStatus.values()) m.put(s.name(), claims.countByStatus(s));
        m.put("TOTAL", claims.count());
        return m;
    }

    /** Customers can only touch their own claims; employees can touch any. */
    private Claim loadAuthorized(AppUser user, Long id) {
        Claim c = claims.findById(id).orElseThrow(() -> notFound("Claim not found"));
        if (user.getRole() == Role.CUSTOMER && !c.getCustomer().getId().equals(user.getId())) {
            throw notFound("Claim not found"); // 404, not 403: don't reveal that the claim exists
        }
        return c;
    }

    private ClaimResponse toResponse(Claim c) {
        return ClaimResponse.of(c, documents.findByClaim(c));
    }

    private static ResponseStatusException notFound(String m) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, m);
    }

    private static String sanitize(String name) {
        if (name == null || name.isBlank()) return "file";
        String base = name.replace("\\", "/");
        base = base.substring(base.lastIndexOf('/') + 1);
        return base.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
