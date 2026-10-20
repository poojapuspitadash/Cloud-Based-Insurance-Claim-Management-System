package com.claimsys.repository;

import com.claimsys.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ClaimRepository extends JpaRepository<Claim, Long> {
    List<Claim> findByCustomerOrderByCreatedAtDesc(AppUser customer);

    @Query("""
        select c from Claim c
        where c.status in :statuses
          and (lower(c.claimNumber) like lower(concat('%', :q, '%'))
               or lower(c.customer.name) like lower(concat('%', :q, '%'))
               or lower(coalesce(c.customer.policyNumber, '')) like lower(concat('%', :q, '%')))
        order by c.createdAt desc
        """)
    List<Claim> search(@Param("statuses") List<ClaimStatus> statuses, @Param("q") String q);

    long countByStatus(ClaimStatus status);
}
