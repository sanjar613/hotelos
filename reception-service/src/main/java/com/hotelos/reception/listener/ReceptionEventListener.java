package com.hotelos.reception.listener;

import com.hotelos.reception.model.RoomStatus;
import com.hotelos.reception.repository.RoomRepository;
import com.hotelos.reception.service.ReceptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReceptionEventListener {

    private final ReceptionService receptionService;
    private final RoomRepository roomRepository;

    /**
     * When Housekeeping marks a room CLEAN → update Reception's own Room record.
     * Without this, the assignment algorithm would never see the room again after checkout.
     * This is the fix for the "room disappears after checkout" bug (Bug #2 in Task 4).
     */
    @RabbitListener(queues = "queue.room.status.changed")
    @Transactional
    public void onRoomStatusChanged(Map<String, Object> payload) {
        try {
            String roomNumber = (String) payload.get("roomNumber");
            String newStatus  = (String) payload.get("newStatus");
            if (roomNumber == null || newStatus == null) return;

            if ("CLEAN".equals(newStatus)) {
                roomRepository.findByRoomNumber(roomNumber).ifPresent(room -> {
                    room.setStatus(RoomStatus.CLEAN);
                    room.setCleanedAt(LocalDateTime.now());
                    roomRepository.save(room);
                    log.info("Room {} marked CLEAN in Reception DB — available for assignment", roomNumber);
                });
            } else if ("BEING_CLEANED".equals(newStatus)) {
                roomRepository.findByRoomNumber(roomNumber).ifPresent(room -> {
                    room.setStatus(RoomStatus.BEING_CLEANED);
                    roomRepository.save(room);
                });
            }
        } catch (Exception e) {
            log.error("Failed to process room.status.changed: {}", e.getMessage());
        }
    }

    /** Room Service charge event — adds to guest bill */
    @RabbitListener(queues = "queue.room.service.charge")
    public void onRoomServiceCharge(Map<String, Object> payload) {
        try {
            String roomNumber = (String) payload.get("roomNumber");
            double amount = ((Number) payload.get("amount")).doubleValue();
            receptionService.addRoomServiceCharge(roomNumber, amount);
        } catch (Exception e) {
            log.error("Failed to process charge event: {}", e.getMessage());
        }
    }
}
