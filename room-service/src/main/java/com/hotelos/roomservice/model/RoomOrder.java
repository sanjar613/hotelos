package com.hotelos.roomservice.model;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity @Table(name="room_orders")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RoomOrder {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private String roomNumber;
    @ElementCollection
    @CollectionTable(name="order_items", joinColumns=@JoinColumn(name="order_id"))
    @Column(name="item") private List<String> items;
    @Column(nullable=false) private Double totalAmount;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private OrderStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @PrePersist protected void onCreate() { createdAt=updatedAt=LocalDateTime.now(); }
    @PreUpdate  protected void onUpdate() { updatedAt=LocalDateTime.now(); }
}
