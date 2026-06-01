package com.hotelos.maintenance.model;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name="maintenance_issues")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MaintenanceIssue {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private String roomNumber;
    @Column(nullable=false) private String description;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private UrgencyLevel urgency;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private IssueStatus status;
    private String assignedTechnician;
    @Column(nullable=false) private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;

    @PrePersist protected void onCreate() { if(createdAt==null) createdAt=LocalDateTime.now(); }
}
