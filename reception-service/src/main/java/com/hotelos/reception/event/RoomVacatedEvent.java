package com.hotelos.reception.event;
import lombok.*;
import java.time.LocalDateTime;

/** Published when guest checks out. Housekeeping subscribes. No payment data included. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RoomVacatedEvent {
    private String roomNumber;
    private Integer floor;
    private String roomType;
    private LocalDateTime vacatedAt;
    private String formerGuestName; // name only — no payment/passport data
}
