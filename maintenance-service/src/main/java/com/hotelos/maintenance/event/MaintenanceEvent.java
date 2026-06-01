package com.hotelos.maintenance.event;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MaintenanceEvent {
    private Long issueId;
    private String roomNumber;
    private String description;
    private String urgency;
    private String status;
    private String assignedTechnician;
    private LocalDateTime changedAt;
}
