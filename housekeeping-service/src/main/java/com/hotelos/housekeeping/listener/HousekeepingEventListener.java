package com.hotelos.housekeeping.listener;
import com.hotelos.housekeeping.event.RoomVacatedEvent;
import com.hotelos.housekeeping.service.HousekeepingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component @RequiredArgsConstructor @Slf4j
public class HousekeepingEventListener {
    private final HousekeepingService svc;

    @RabbitListener(queues = "queue.room.vacated")
    public void onRoomVacated(RoomVacatedEvent event) {
        log.info("Received room.vacated: room={}", event.getRoomNumber());
        try { svc.onRoomVacated(event); }
        catch (Exception e) { log.error("Failed processing room.vacated for {}: {}", event.getRoomNumber(), e.getMessage()); }
    }
}
