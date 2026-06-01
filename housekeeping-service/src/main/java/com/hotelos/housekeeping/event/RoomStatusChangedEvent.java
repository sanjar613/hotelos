package com.hotelos.housekeeping.event;
import lombok.*;
import java.time.LocalDateTime;
/** Published on each room status transition. Received by dashboard via WebSocket. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RoomStatusChangedEvent {
    private String roomNumber;
    private Integer floor;
    private String oldStatus;
    private String newStatus;
    private String assignedHousekeeper;
    private LocalDateTime changedAt;
}
