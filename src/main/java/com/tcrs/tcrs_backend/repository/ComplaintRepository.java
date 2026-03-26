package com.tcrs.tcrs_backend.repository;


import com.tcrs.tcrs_backend.model.Complaint;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComplaintRepository extends MongoRepository<Complaint, String> {
    // get all complaints by a specific citizen
    List<Complaint> findBySubmittedByEmail(String email);

    // get complaints by status
    List<Complaint> findByStatus(String status);

    // get complaints by category
    List<Complaint> findByCategory(String category);

    // count by status (for dashboard)
    long countByStatus(String status);
}