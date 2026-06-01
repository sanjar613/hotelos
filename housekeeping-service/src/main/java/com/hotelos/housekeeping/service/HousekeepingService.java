package com.hotelos.housekeeping.service;
import com.hotelos.housekeeping.config.HotelOSEvents;
import com.hotelos.housekeeping.event.*;
import com.hotelos.housekeeping.model.*;
import com.hotelos.housekeeping.repository.HkRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service @RequiredArgsConstructor @Slf4j
public class HousekeepingService {

    private final HkRoomRepository repo;
    private final RabbitTemplate rabbit;
    private final SimpMessagingTemplate ws;

    /** EVENT-DRIVEN: called by RabbitMQ listener when Reception publishes room.vacated */
    @Transactional
    public void onRoomVacated(RoomVacatedEvent event) {
        log.info("room.vacated received: room {}", event.getRoomNumber());
        HkRoom room = repo.findById(event.getRoomNumber()).orElse(
            HkRoom.builder().roomNumber(event.getRoomNumber()).floor(event.getFloor()).build());
        String old = room.getStatus() != null ? room.getStatus().name() : "UNKNOWN";
        room.setStatus(HkStatus.DIRTY);
        room.setVacatedAt(event.getVacatedAt());
        room.setCleanedAt(null);
        repo.save(room);
        publishAndPush(room, old, HkStatus.DIRTY.name(), null);
        log.info("Room {} added to cleaning queue", event.getRoomNumber());
    }

    /** TS-03 Step 1: DIRTY → BEING_CLEANED */
    @Transactional
    public HkRoom startCleaning(String roomNumber, String housekeeper) {
        HkRoom room = get(roomNumber);
        assertStatus(room, HkStatus.DIRTY, HkStatus.BEING_CLEANED);
        String old = room.getStatus().name();
        room.setStatus(HkStatus.BEING_CLEANED);
        room.setAssignedHousekeeper(housekeeper);
        room.setCleaningStartedAt(LocalDateTime.now());
        repo.save(room);
        publishAndPush(room, old, HkStatus.BEING_CLEANED.name(), housekeeper);
        return room;
    }

    /** TS-03 Step 2: BEING_CLEANED → CLEAN (dashboard updates via WebSocket) */
    @Transactional
    public HkRoom markClean(String roomNumber) {
        HkRoom room = get(roomNumber);
        assertStatus(room, HkStatus.BEING_CLEANED, HkStatus.CLEAN);
        String old = room.getStatus().name();
        room.setStatus(HkStatus.CLEAN);
        room.setCleanedAt(LocalDateTime.now());
        repo.save(room);
        publishAndPush(room, old, HkStatus.CLEAN.name(), room.getAssignedHousekeeper());
        log.info("Room {} is CLEAN — available for assignment", roomNumber);
        return room;
    }

    public List<HkRoom> getDirtyRooms() { return repo.findByStatus(HkStatus.DIRTY); }
    public List<HkRoom> getAllRooms()    { return repo.findAllByOrderByRoomNumber(); }

    /** Dual-publish: broker (other services) + WebSocket (dashboard live update) */
    private void publishAndPush(HkRoom room, String old, String next, String keeper) {
        RoomStatusChangedEvent ev = RoomStatusChangedEvent.builder()
            .roomNumber(room.getRoomNumber()).floor(room.getFloor())
            .oldStatus(old).newStatus(next).assignedHousekeeper(keeper)
            .changedAt(LocalDateTime.now()).build();
        rabbit.convertAndSend(HotelOSEvents.EXCHANGE, HotelOSEvents.RK_ROOM_STATUS, ev);
        ws.convertAndSend("/topic/rooms", ev);  // WebSocket push — no page refresh needed
        log.debug("Room {} {} → {}", room.getRoomNumber(), old, next);
    }

    private HkRoom get(String roomNumber) {
        return repo.findById(roomNumber)
            .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomNumber));
    }

    private void assertStatus(HkRoom room, HkStatus expected, HkStatus target) {
        if (room.getStatus() != expected)
            throw new IllegalStateException(String.format(
                "Cannot move room %s to %s — current status is %s (expected %s)",
                room.getRoomNumber(), target, room.getStatus(), expected));
    }
}
