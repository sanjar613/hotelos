package com.hotelos.reception.dto;
import lombok.*;
@Data @Builder
public class CheckInResponse {
    private Long bookingId;
    private String guestName;
    private String assignedRoom;
    private Integer floor;
    private String roomType;
    private Double nightlyRate;
    private String checkInDate;
    private String message;
}
