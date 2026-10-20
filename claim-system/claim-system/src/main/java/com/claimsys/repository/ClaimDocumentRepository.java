package com.claimsys.repository;

import com.claimsys.model.Claim;
import com.claimsys.model.ClaimDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ClaimDocumentRepository extends JpaRepository<ClaimDocument, Long> {
    List<ClaimDocument> findByClaim(Claim claim);
}
