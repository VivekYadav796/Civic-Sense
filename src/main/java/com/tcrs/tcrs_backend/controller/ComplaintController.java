package com.tcrs.tcrs_backend.controller;


import com.tcrs.tcrs_backend.dto.ComplaintDTO;
import com.tcrs.tcrs_backend.model.Complaint;
import com.tcrs.tcrs_backend.model.User;
import com.tcrs.tcrs_backend.repository.ComplaintRepository;
import com.tcrs.tcrs_backend.repository.UserRepository;
import com.tcrs.tcrs_backend.service.AuditLogService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/complaints")
public class ComplaintController {

    @Autowired private ComplaintRepository complaintRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private AuditLogService auditLogService;

    // ── Submit a new complaint (citizen) ───────────────────────────────────────
    @PostMapping
    public ResponseEntity<?> submit(@Valid @RequestBody ComplaintDTO.CreateRequest req,
                                    Authentication auth) {
        String email = auth.getName();
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) return ResponseEntity.status(404).body("User not found");

        User user = userOpt.get();

        Complaint complaint = new Complaint();
        complaint.setTitle(req.getTitle());
        complaint.setDescription(req.getDescription());
        complaint.setCategory(req.getCategory());
        complaint.setLocation(req.getLocation());
        complaint.setStatus("PENDING");
        complaint.setSubmittedByEmail(email);
        complaint.setSubmittedByName(user.getName());

        complaintRepository.save(complaint);

        auditLogService.log(email, "COMPLAINT_CREATED", "COMPLAINT",
            complaint.getId(), "Complaint submitted: " + complaint.getTitle());

        return ResponseEntity.ok(complaint);
    }

    // ── Get my complaints (citizen sees only their own) ────────────────────────
    @GetMapping("/my")
    public ResponseEntity<?> myComplaints(Authentication auth) {
        List<Complaint> list = complaintRepository.findBySubmittedByEmail(auth.getName());
        return ResponseEntity.ok(list);
    }

    // ── Get single complaint by ID ─────────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable String id, Authentication auth) {
        Optional<Complaint> opt = complaintRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        Complaint c = opt.get();
        String email = auth.getName();
        String role  = auth.getAuthorities().iterator().next().getAuthority();

        // citizens can only see their own
        if (role.equals("ROLE_CITIZEN") && !c.getSubmittedByEmail().equals(email)) {
            return ResponseEntity.status(403).body("Access denied");
        }

        return ResponseEntity.ok(c);
    }

    // ── Get ALL complaints (admin / auditor only) ──────────────────────────────
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN','AUDITOR')")
    public ResponseEntity<?> getAllComplaints() {
        return ResponseEntity.ok(complaintRepository.findAll());
    }

    // ── Update complaint status (admin only) ───────────────────────────────────
    @PatchMapping("/admin/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateStatus(@PathVariable String id,
                                          @Valid @RequestBody ComplaintDTO.UpdateStatusRequest req,
                                          Authentication auth) {
        Optional<Complaint> opt = complaintRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        Complaint complaint = opt.get();
        String oldStatus = complaint.getStatus();

        complaint.setStatus(req.getStatus());
        complaint.setAssignedToEmail(auth.getName());
        if (req.getAdminRemarks() != null) {
            complaint.setAdminRemarks(req.getAdminRemarks());
        }

        complaintRepository.save(complaint);

        auditLogService.log(
            auth.getName(),
            "STATUS_UPDATED",
            "COMPLAINT",
            id,
            "Status changed from " + oldStatus + " to " + req.getStatus()
        );

        return ResponseEntity.ok(complaint);
    }

    // ── Delete complaint (admin only) ──────────────────────────────────────────
    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> delete(@PathVariable String id, Authentication auth) {
        if (!complaintRepository.existsById(id)) return ResponseEntity.notFound().build();

        complaintRepository.deleteById(id);

        auditLogService.log(auth.getName(), "COMPLAINT_DELETED", "COMPLAINT", id,
            "Complaint deleted by admin");

        return ResponseEntity.ok("Complaint deleted");
    }

    // ── Dashboard stats (admin only) ───────────────────────────────────────────
    @GetMapping("/admin/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> stats() {
        long total    = complaintRepository.count();
        long pending  = complaintRepository.countByStatus("PENDING");
        long inProg   = complaintRepository.countByStatus("IN_PROGRESS");
        long resolved = complaintRepository.countByStatus("RESOLVED");
        long rejected = complaintRepository.countByStatus("REJECTED");

        return ResponseEntity.ok(java.util.Map.of(
            "total",     total,
            "pending",   pending,
            "inProgress",inProg,
            "resolved",  resolved,
            "rejected",  rejected
        ));
    }
}
