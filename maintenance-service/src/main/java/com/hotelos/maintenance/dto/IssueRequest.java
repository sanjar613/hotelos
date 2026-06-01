package com.hotelos.maintenance.dto;
import com.hotelos.maintenance.model.UrgencyLevel;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class IssueRequest {
    @NotBlank(message="Room number required") private String roomNumber;
    @NotBlank(message="Description required")
    @Size(min=5,max=500) private String description;
    @NotNull(message="Urgency required") private UrgencyLevel urgency;
    private String reportedBy;
}
