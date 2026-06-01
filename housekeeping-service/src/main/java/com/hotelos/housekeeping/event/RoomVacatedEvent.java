package com.hotelos.housekeeping.event;
import lombok.*;
import java.time.LocalDateTime;
/** Received from Reception. No payment data included. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RoomVacatedEvent {
    private String roomNumber;
    private Integer floor;
    private String roomType;
    private LocalDateTime vacatedAt;
    private String formerGuestName;
}
