package com.tcrs.tcrs_backend.model;


import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "complaints")
public class Complaint{

    @Id
    private String id;

    private String title;

    private String description;

    // e.g. "ROAD", "WATER", "ELECTRICITY", "GARBAGE", "SAFETY", "OTHER"
    private String category;

    private String location;

    // "PENDING", "IN_PROGRESS", "RESOLVED", "REJECTED"
    private String status;

    // who submitted this complaint
    private String submittedByEmail;
    private String submittedByName;

    // admin who is handling it
    private String assignedToEmail;

    // admin's response/remarks
    private String adminRemarks;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}