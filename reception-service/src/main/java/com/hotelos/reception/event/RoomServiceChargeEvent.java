package com.hotelos.reception.event;
import lombok.*;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RoomServiceChargeEvent {
    private String roomNumber;
    private Double amount;
    private Long orderId;
}
