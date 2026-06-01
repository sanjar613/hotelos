package com.hotelos.maintenance.service;
import com.hotelos.maintenance.config.HotelOSEvents;
import com.hotelos.maintenance.event.MaintenanceEvent;
import com.hotelos.maintenance.model.*;
import com.hotelos.maintenance.repository.MaintenanceRepository;
import com.hotelos.maintenance.dto.IssueRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service @RequiredArgsConstructor @Slf4j
public class MaintenanceService {

    private final MaintenanceRepository repo;
    private final RabbitTemplate rabbit;
    private final SimpMessagingTemplate ws;

    // Static list of available technicians — in production this would come from a DB
    private static final List<String> TECHNICIANS = List.of("Tom","Alex","Jordan","Sam");
    private int techIndex = 0;

    /**
     * ═══════════════════════════════════════════════════════════════
     * PRIORITY QUEUE ALGORITHM — reportIssue
     * ═══════════════════════════════════════════════════════════════
     * Step 1 — Save issue with urgency and timestamp
     * Step 2 — Assign next available technician (round-robin)
     * Step 3 — Sort queue: urgency ASC (CRITICAL first), then createdAt ASC (FIFO tiebreak)
     *          The DB query handles this automatically via ORDER BY urgency ASC, createdAt ASC
     *          because UrgencyLevel ordinal maps: CRITICAL=0, HIGH=1, NORMAL=2, LOW=3
     * Step 4 — Publish event + WebSocket push
     *
     * TS-05: Critical issue goes to front because urgency=CRITICAL has ordinal 0 (lowest value)
     */
    @Transactional
    public MaintenanceIssue reportIssue(IssueRequest req) {
        String tech = assignNextTechnician();
        MaintenanceIssue issue = MaintenanceIssue.builder()
            .roomNumber(req.getRoomNumber())
            .description(req.getDescription())
            .urgency(req.getUrgency())
            .status(IssueStatus.ASSIGNED)
            .assignedTechnician(tech)
            .createdAt(LocalDateTime.now()).build();
        issue = repo.save(issue);

        publishAndPush(issue, HotelOSEvents.RK_MAINT_NEW);
        log.info("Issue #{} ({}) in room {} assigned to {}", issue.getId(), issue.getUrgency(), issue.getRoomNumber(), tech);
        return issue;
    }

    /** Mark an issue resolved — publishes event so dashboard updates */
    @Transactional
    public MaintenanceIssue resolveIssue(Long id) {
        MaintenanceIssue issue = repo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Issue not found: " + id));
        if (issue.getStatus() == IssueStatus.RESOLVED)
            throw new IllegalStateException("Issue " + id + " already resolved");
        issue.setStatus(IssueStatus.RESOLVED);
        issue.setResolvedAt(LocalDateTime.now());
        issue = repo.save(issue);
        publishAndPush(issue, HotelOSEvents.RK_MAINT_DONE);
        log.info("Issue #{} resolved", id);
        return issue;
    }

    /** Returns priority queue — CRITICAL first, FIFO within same level */
    public List<MaintenanceIssue> getQueue() { return repo.findQueueOrdered(); }
    public List<MaintenanceIssue> getAll()   { return repo.findAllByOrderByUrgencyAscCreatedAtAsc(); }

    private String assignNextTechnician() {
        String tech = TECHNICIANS.get(techIndex % TECHNICIANS.size());
        techIndex++;
        return tech;
    }

    private void publishAndPush(MaintenanceIssue issue, String routingKey) {
        MaintenanceEvent ev = MaintenanceEvent.builder()
            .issueId(issue.getId()).roomNumber(issue.getRoomNumber())
            .description(issue.getDescription()).urgency(issue.getUrgency().name())
            .status(issue.getStatus().name()).assignedTechnician(issue.getAssignedTechnician())
            .changedAt(LocalDateTime.now()).build();
        rabbit.convertAndSend(HotelOSEvents.EXCHANGE, routingKey, ev);
        ws.convertAndSend("/topic/maintenance", ev);
    }
}
