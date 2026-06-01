package com.hotelos.roomservice.event;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class OrderStatusChangedEvent {
    private Long orderId;
    private String roomNumber;
    private String oldStatus;
    private String newStatus;
    private List<String> items;
    private Double totalAmount;
    private LocalDateTime changedAt;
}
