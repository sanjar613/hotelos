package com.hotelos.reception.service;
import com.hotelos.reception.model.*;
import com.hotelos.reception.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.*;

/**
 * ═══════════════════════════════════════════════════════════════
 * ROOM ASSIGNMENT ALGORITHM
 * ═══════════════════════════════════════════════════════════════
 * Step 1 – Type + cleanliness filter (DB query: CLEAN rooms of requested type)
 * Step 2 – Sort by longest-clean-first (cleanedAt ASC)
 * Step 3 – Floor preference filter (use preferred floor; fallback to all if none available)
 * Step 4 – Proximity tiebreaker (nearElevator > nearStairs > none)
 * Step 5 – Return first candidate (deterministic, fair rotation)
 *
 * Complexity: O(n log n), n = number of CLEAN rooms of requested type (≤10 in simplified build).
 *
 * Alternatives rejected:
 *  • Weighted scoring: subjective weights, non-deterministic ties.
 *  • Random selection: doesn't enforce even rotation.
 *  • Full SQL ORDER BY: floor-fallback logic not expressible in single query.
 */
@Service @RequiredArgsConstructor @Slf4j
public class RoomAssignmentService {

    private final RoomRepository roomRepository;

    public Optional<Room> findBestRoom(RoomType type, Integer preferredFloor,
                                       boolean nearElevator, boolean nearStairs) {
        log.info("Assignment: type={} floor={} elevator={} stairs={}", type, preferredFloor, nearElevator, nearStairs);

        // STEP 1 & 2: DB returns CLEAN rooms sorted by cleanedAt ASC
        List<Room> eligible = roomRepository.findEligible(RoomStatus.CLEAN, type);
        if (eligible.isEmpty()) { log.warn("No CLEAN {} rooms available", type); return Optional.empty(); }

        // STEP 3: Floor preference
        List<Room> candidates = applyFloorPreference(eligible, preferredFloor);

        // STEP 4: Proximity tiebreaker (stable sort preserves cleanedAt order within groups)
        candidates = applyProximity(candidates, nearElevator, nearStairs);

        // STEP 5: First = best
        Room best = candidates.get(0);
        log.info("Assigned room {} (floor {}, cleanedAt {})", best.getRoomNumber(), best.getFloor(), best.getCleanedAt());
        return Optional.of(best);
    }

    private List<Room> applyFloorPreference(List<Room> eligible, Integer floor) {
        if (floor == null) return eligible;
        List<Room> onFloor = eligible.stream().filter(r -> r.getFloor().equals(floor)).toList();
        if (onFloor.isEmpty()) { log.info("No CLEAN rooms on floor {} — falling back to any floor", floor); return eligible; }
        return onFloor;
    }

    private List<Room> applyProximity(List<Room> candidates, boolean elevator, boolean stairs) {
        if (!elevator && !stairs) return candidates;
        return candidates.stream()
            .sorted(Comparator.comparingInt(r -> {
                if (elevator && r.isNearElevator()) return 0;
                if (stairs   && r.isNearStairs())   return 1;
                return 2;
            }))
            .toList();
    }
}
