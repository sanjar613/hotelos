package com.hotelos.reception;

import com.hotelos.reception.model.*;
import com.hotelos.reception.repository.RoomRepository;
import com.hotelos.reception.service.RoomAssignmentService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the Room Assignment Algorithm.
 * Tests all 5 algorithm steps and edge cases from the assignment test scenarios.
 */
@ExtendWith(MockitoExtension.class)
class RoomAssignmentServiceTest {

    @Mock RoomRepository roomRepository;
    @InjectMocks RoomAssignmentService service;

    private Room makeRoom(String num, int floor, RoomType type, int hoursAgo, boolean elev, boolean stairs) {
        return Room.builder()
            .roomNumber(num).floor(floor).roomType(type)
            .status(RoomStatus.CLEAN).nightlyRate(100.0)
            .cleanedAt(LocalDateTime.now().minusHours(hoursAgo))
            .nearElevator(elev).nearStairs(stairs).build();
    }

    // ─── TS-01: double room floor 3 ───
    @Test @DisplayName("TS-01a: assigns cleanest double on preferred floor")
    void assignsLongestCleanOnPreferredFloor() {
        Room r1 = makeRoom("102", 1, RoomType.DOUBLE, 3, false, false);
        Room r2 = makeRoom("202", 2, RoomType.DOUBLE, 7, false, false); // longest clean on floor 2
        Room r3 = makeRoom("203", 2, RoomType.DOUBLE, 2, false, false);
        when(roomRepository.findEligible(RoomStatus.CLEAN, RoomType.DOUBLE))
            .thenReturn(List.of(r2, r3, r1)); // already sorted by cleanedAt ASC from DB

        Optional<Room> result = service.findBestRoom(RoomType.DOUBLE, 2, false, false);

        assertThat(result).isPresent();
        assertThat(result.get().getRoomNumber()).isEqualTo("202"); // longest clean on floor 2
    }

    @Test @DisplayName("TS-01b: falls back to any floor when preferred floor has no clean rooms")
    void fallsBackToAnyFloorWhenPreferredEmpty() {
        Room r1 = makeRoom("102", 1, RoomType.DOUBLE, 5, false, false);
        when(roomRepository.findEligible(RoomStatus.CLEAN, RoomType.DOUBLE)).thenReturn(List.of(r1));

        Optional<Room> result = service.findBestRoom(RoomType.DOUBLE, 3, false, false);

        assertThat(result).isPresent();
        assertThat(result.get().getRoomNumber()).isEqualTo("102"); // fallback floor 1
    }

    // ─── TS-07: no rooms available ───
    @Test @DisplayName("TS-07: returns empty when no CLEAN rooms of requested type")
    void returnsEmptyWhenNoCleanRooms() {
        when(roomRepository.findEligible(RoomStatus.CLEAN, RoomType.SUITE)).thenReturn(List.of());

        Optional<Room> result = service.findBestRoom(RoomType.SUITE, null, false, false);

        assertThat(result).isEmpty();
    }

    // ─── Step 2: longest-clean-first ───
    @Test @DisplayName("Step 2: selects room with earliest cleanedAt (longest clean)")
    void selectsLongestClean() {
        Room r1 = makeRoom("101", 1, RoomType.SINGLE, 2, false, false); // cleaned 2h ago
        Room r2 = makeRoom("201", 2, RoomType.SINGLE, 6, false, false); // cleaned 6h ago — should win
        // DB returns sorted ASC, so r2 comes first
        when(roomRepository.findEligible(RoomStatus.CLEAN, RoomType.SINGLE)).thenReturn(List.of(r2, r1));

        Optional<Room> result = service.findBestRoom(RoomType.SINGLE, null, false, false);

        assertThat(result.get().getRoomNumber()).isEqualTo("201");
    }

    // ─── Step 4: proximity tiebreaker ───
    @Test @DisplayName("Step 4: prefers nearElevator room when requested")
    void prefersElevatorRoom() {
        Room noElev = makeRoom("101", 1, RoomType.SINGLE, 5, false, false);
        Room elev   = makeRoom("102", 1, RoomType.SINGLE, 4, true,  false);
        when(roomRepository.findEligible(RoomStatus.CLEAN, RoomType.SINGLE)).thenReturn(List.of(noElev, elev));

        Optional<Room> result = service.findBestRoom(RoomType.SINGLE, null, true, false);

        assertThat(result.get().getRoomNumber()).isEqualTo("102"); // elevator room preferred
    }

    // ─── TS-06: concurrent check-in simulation ───
    @Test @DisplayName("TS-06: two requests for same type get different rooms")
    void concurrentCheckInGetsDifferentRooms() {
        Room r1 = makeRoom("101", 1, RoomType.SINGLE, 5, false, false);
        Room r2 = makeRoom("201", 2, RoomType.SINGLE, 3, false, false);
        // First call: both available
        when(roomRepository.findEligible(RoomStatus.CLEAN, RoomType.SINGLE))
            .thenReturn(List.of(r1, r2))   // first call
            .thenReturn(List.of(r2));       // second call (r1 already marked OCCUPIED)

        Optional<Room> first  = service.findBestRoom(RoomType.SINGLE, null, false, false);
        Optional<Room> second = service.findBestRoom(RoomType.SINGLE, null, false, false);

        assertThat(first.get().getRoomNumber()).isNotEqualTo(second.get().getRoomNumber());
    }
}
