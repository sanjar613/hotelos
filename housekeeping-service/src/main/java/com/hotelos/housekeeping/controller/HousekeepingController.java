package com.hotelos.housekeeping.controller;
import com.hotelos.housekeeping.model.HkRoom;
import com.hotelos.housekeeping.service.HousekeepingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/housekeeping") @RequiredArgsConstructor
public class HousekeepingController {
    private final HousekeepingService svc;

    @GetMapping("/queue")  public ResponseEntity<List<HkRoom>> queue()    { return ResponseEntity.ok(svc.getDirtyRooms()); }
    @GetMapping("/rooms")  public ResponseEntity<List<HkRoom>> all()      { return ResponseEntity.ok(svc.getAllRooms()); }

    /** TS-03: housekeeper starts cleaning */
    @PostMapping("/rooms/{room}/start-cleaning")
    public ResponseEntity<HkRoom> startCleaning(@PathVariable String room, @RequestBody Map<String,String> body) {
        return ResponseEntity.ok(svc.startCleaning(room, body.getOrDefault("housekeeper","Unknown")));
    }

    /** TS-03: housekeeper marks done — dashboard updates live via WebSocket */
    @PostMapping("/rooms/{room}/mark-clean")
    public ResponseEntity<HkRoom> markClean(@PathVariable String room) {
        return ResponseEntity.ok(svc.markClean(room));
    }
}
