package com.hotelos.reception.dto;
import lombok.*;
@Data @Builder
public class CheckOutResponse {
    private Long bookingId;
    private String guestName;
    private String roomNumber;
    private int numberOfNights;
    private Double nightlyRate;
    private Double roomChargesSubtotal;
    private Double roomServiceCharges;
    private Double additionalCharges;
    private Double discountAmount;
    private Double totalBill;
    private String message;
}
