package com.hotelos.maintenance.controller;
import com.hotelos.maintenance.dto.IssueRequest;
import com.hotelos.maintenance.model.MaintenanceIssue;
import com.hotelos.maintenance.service.MaintenanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/maintenance") @RequiredArgsConstructor
public class MaintenanceController {
    private final MaintenanceService svc;

    /** TS-05: report maintenance issue with urgency */
    @PostMapping("/issues")
    public ResponseEntity<MaintenanceIssue> report(@Valid @RequestBody IssueRequest req) {
        return ResponseEntity.ok(svc.reportIssue(req));
    }

    /** TS-05: resolve issue — dashboard updates via WebSocket */
    @PostMapping("/issues/{id}/resolve")
    public ResponseEntity<MaintenanceIssue> resolve(@PathVariable Long id) {
        return ResponseEntity.ok(svc.resolveIssue(id));
    }

    /** Priority queue — CRITICAL first */
    @GetMapping("/queue")
    public ResponseEntity<List<MaintenanceIssue>> queue() { return ResponseEntity.ok(svc.getQueue()); }

    /** All issues */
    @GetMapping("/issues")
    public ResponseEntity<List<MaintenanceIssue>> all() { return ResponseEntity.ok(svc.getAll()); }
}
