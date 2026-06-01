package com.hotelos.reception.controller;
import com.hotelos.reception.dto.*;
import com.hotelos.reception.model.Room;
import com.hotelos.reception.service.ReceptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/** Reception REST API — all endpoints require HTTP Basic Auth. */
@RestController @RequestMapping("/api/reception") @RequiredArgsConstructor
public class ReceptionController {
    private final ReceptionService receptionService;

    /** TS-01, TS-06, TS-07, TS-08 */
    @PostMapping("/checkin")
    public ResponseEntity<CheckInResponse> checkIn(@Valid @RequestBody CheckInRequest req) {
        return ResponseEntity.ok(receptionService.checkIn(req));
    }

    /** TS-02 */
    @PostMapping("/checkout/{roomNumber}")
    public ResponseEntity<CheckOutResponse> checkOut(@PathVariable String roomNumber) {
        return ResponseEntity.ok(receptionService.checkOut(roomNumber));
    }

    /** Dashboard room inventory */
    @GetMapping("/rooms")
    public ResponseEntity<List<Room>> rooms() {
        return ResponseEntity.ok(receptionService.getAllRooms());
    }
}
