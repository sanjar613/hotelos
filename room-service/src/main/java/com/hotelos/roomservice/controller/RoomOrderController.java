package com.hotelos.roomservice.controller;
import com.hotelos.roomservice.dto.OrderRequest;
import com.hotelos.roomservice.model.RoomOrder;
import com.hotelos.roomservice.service.RoomOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/room-service") @RequiredArgsConstructor
public class RoomOrderController {
    private final RoomOrderService svc;

    /** TS-04: place order */
    @PostMapping("/orders")
    public ResponseEntity<RoomOrder> place(@Valid @RequestBody OrderRequest req) {
        return ResponseEntity.ok(svc.placeOrder(req));
    }

    /** TS-04: advance order to next state */
    @PostMapping("/orders/{id}/advance")
    public ResponseEntity<RoomOrder> advance(@PathVariable Long id) {
        return ResponseEntity.ok(svc.advance(id));
    }

    /** Dashboard: all active (non-delivered) orders */
    @GetMapping("/orders/active")
    public ResponseEntity<List<RoomOrder>> active() { return ResponseEntity.ok(svc.getActive()); }

    /** Orders for a specific room */
    @GetMapping("/orders/room/{roomNumber}")
    public ResponseEntity<List<RoomOrder>> byRoom(@PathVariable String roomNumber) {
        return ResponseEntity.ok(svc.getByRoom(roomNumber));
    }
}
