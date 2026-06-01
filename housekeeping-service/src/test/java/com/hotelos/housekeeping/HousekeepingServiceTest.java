package com.hotelos.housekeeping;

import com.hotelos.housekeeping.event.RoomVacatedEvent;
import com.hotelos.housekeeping.model.*;
import com.hotelos.housekeeping.repository.HkRoomRepository;
import com.hotelos.housekeeping.service.HousekeepingService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HousekeepingServiceTest {

    @Mock HkRoomRepository repo;
    @Mock RabbitTemplate rabbit;
    @Mock SimpMessagingTemplate ws;
    @InjectMocks HousekeepingService svc;

    private HkRoom dirtyRoom(String num) {
        return HkRoom.builder().roomNumber(num).floor(2).status(HkStatus.DIRTY).build();
    }

    @Test @DisplayName("TS-02: room.vacated event adds room to DIRTY queue")
    void vacatedEventMakesRoomDirty() {
        RoomVacatedEvent ev = RoomVacatedEvent.builder()
            .roomNumber("204").floor(2).roomType("SUITE").vacatedAt(LocalDateTime.now()).build();
        when(repo.findById("204")).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(i->i.getArgument(0));

        svc.onRoomVacated(ev);

        ArgumentCaptor<HkRoom> cap = ArgumentCaptor.forClass(HkRoom.class);
        verify(repo).save(cap.capture());
        assertThat(cap.getValue().getStatus()).isEqualTo(HkStatus.DIRTY);
        assertThat(cap.getValue().getRoomNumber()).isEqualTo("204");
    }

    @Test @DisplayName("TS-03: DIRTY → BEING_CLEANED transition")
    void startCleaning() {
        when(repo.findById("204")).thenReturn(Optional.of(dirtyRoom("204")));
        when(repo.save(any())).thenAnswer(i->i.getArgument(0));

        HkRoom result = svc.startCleaning("204", "Maria");

        assertThat(result.getStatus()).isEqualTo(HkStatus.BEING_CLEANED);
        assertThat(result.getAssignedHousekeeper()).isEqualTo("Maria");
        verify(ws).convertAndSend(eq("/topic/rooms"), any());
    }

    @Test @DisplayName("TS-03: BEING_CLEANED → CLEAN, WebSocket broadcast sent")
    void markClean() {
        HkRoom room = dirtyRoom("204");
        room.setStatus(HkStatus.BEING_CLEANED);
        when(repo.findById("204")).thenReturn(Optional.of(room));
        when(repo.save(any())).thenAnswer(i->i.getArgument(0));

        HkRoom result = svc.markClean("204");

        assertThat(result.getStatus()).isEqualTo(HkStatus.CLEAN);
        assertThat(result.getCleanedAt()).isNotNull();
        verify(ws).convertAndSend(eq("/topic/rooms"), any());   // WebSocket push verified
        verify(rabbit).convertAndSend(any(String.class), any(String.class), any(Object.class));
    }

    @Test @DisplayName("Invalid transition throws IllegalStateException")
    void invalidTransitionThrows() {
        HkRoom room = dirtyRoom("101");
        room.setStatus(HkStatus.CLEAN);
        when(repo.findById("101")).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> svc.startCleaning("101", "Bob"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Cannot move room");
    }

    @Test @DisplayName("Room not found throws IllegalArgumentException")
    void roomNotFoundThrows() {
        when(repo.findById("999")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> svc.startCleaning("999","Maria"))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
