package com.claimsys.controller;

import com.claimsys.dto.Dtos.*;
import com.claimsys.model.AppUser;
import com.claimsys.model.ClaimDocument;
import com.claimsys.repository.UserRepository;
import com.claimsys.service.ClaimService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/** Endpoints available to any logged-in user (customers see only their own claims). */
@RestController
@RequestMapping("/api/claims")
public class ClaimController {
    private final ClaimService service;
    private final UserRepository users;

    public ClaimController(ClaimService service, UserRepository users) {
        this.service = service; this.users = users;
    }

    private AppUser current(Authentication a) { return users.findByEmail(a.getName()).orElseThrow(); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClaimResponse create(Authentication auth, @Valid @RequestBody CreateClaimRequest req) {
        return service.create(current(auth), req);
    }

    @GetMapping
    public List<ClaimResponse> mine(Authentication auth) {
        return service.listForCustomer(current(auth));
    }

    @GetMapping("/{id}")
    public ClaimResponse one(Authentication auth, @PathVariable Long id) {
        return service.get(current(auth), id);
    }

    @PostMapping(path = "/{id}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse upload(Authentication auth, @PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return service.upload(current(auth), id, file);
    }

    @GetMapping("/{id}/documents/{docId}")
    public ResponseEntity<byte[]> download(Authentication auth, @PathVariable Long id, @PathVariable Long docId) {
        Map.Entry<ClaimDocument, byte[]> r = service.download(current(auth), id, docId);
        ClaimDocument d = r.getKey();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(d.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(d.getFileName()).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(r.getValue());
    }
}
