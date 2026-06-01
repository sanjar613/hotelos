package com.hotelos.reception.model;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;


@Entity @Table(name="rooms")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Room {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Column(nullable=false, unique=true) private String roomNumber;
    @Column(nullable=false) private Integer floor;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private RoomType roomType;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private RoomStatus status;
    @Column(nullable=false) private Double nightlyRate;
    private LocalDateTime cleanedAt;
    private boolean nearElevator;
    private boolean nearStairs;
    private String currentGuestName;
    private Long currentBookingId;
}
