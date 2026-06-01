package com.hotelos.reception.dto;
import com.hotelos.reception.model.RoomType;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CheckInRequest {
    @NotBlank(message="Guest name required")
    @Size(min=2,max=100) private String guestName;
    @NotNull(message="Room type required") private RoomType requestedRoomType;
    private Integer preferredFloor;
    private boolean preferNearElevator;
    private boolean preferNearStairs;
    @NotBlank(message="Check-in date required") private String checkInDate;
    @Min(0) @Max(100) private int discountPercent;
}
