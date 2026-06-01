package com.hotelos.roomservice.dto;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;

@Data
public class OrderRequest {
    @NotBlank(message="Room number required") private String roomNumber;
    @NotEmpty(message="Must order at least one item") private List<String> items;
    @Positive(message="Total must be positive") private Double totalAmount;
}
