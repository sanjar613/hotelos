package com.hotelos.reception.model;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity @Table(name="bookings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Booking {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private String guestName;
    @Column(nullable=false) private String roomNumber;
    @Enumerated(EnumType.STRING) private RoomType requestedType;
    @Column(nullable=false) private LocalDate checkInDate;
    private LocalDate checkOutDate;
    @Column(nullable=false) private Double roomServiceCharges;
    @Column(nullable=false) private Double additionalCharges;
    @Column(nullable=false) private Double discountRate;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private BookingStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist protected void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
        if (roomServiceCharges==null) roomServiceCharges=0.0;
        if (additionalCharges==null) additionalCharges=0.0;
        if (discountRate==null) discountRate=0.0;
    }
    @PreUpdate protected void onUpdate() { updatedAt=LocalDateTime.now(); }
}
