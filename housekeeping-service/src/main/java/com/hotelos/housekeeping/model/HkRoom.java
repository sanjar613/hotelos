package com.hotelos.housekeeping.model;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name="hk_rooms")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HkRoom {
    @Id private String roomNumber;
    private Integer floor;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private HkStatus status;
    private String assignedHousekeeper;
    private LocalDateTime vacatedAt;
    private LocalDateTime cleaningStartedAt;
    private LocalDateTime cleanedAt;
}
