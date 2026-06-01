package com.hotelos.reception.service;
import com.hotelos.reception.config.HotelOSEvents;
import com.hotelos.reception.dto.*;
import com.hotelos.reception.event.RoomVacatedEvent;
import com.hotelos.reception.model.*;
import com.hotelos.reception.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.List;
import java.util.Optional;

@Service @RequiredArgsConstructor @Slf4j
public class ReceptionService {

    private final RoomRepository roomRepo;
    private final BookingRepository bookingRepo;
    private final RoomAssignmentService assignmentSvc;
    private final BillingService billingSvc;
    private final RabbitTemplate rabbit;

    @Transactional
    public CheckInResponse checkIn(CheckInRequest req) {
        log.info("Check-in: guest={} type={}", req.getGuestName(), req.getRequestedRoomType());
        Optional<Room> best = assignmentSvc.findBestRoom(
            req.getRequestedRoomType(), req.getPreferredFloor(),
            req.isPreferNearElevator(), req.isPreferNearStairs());

        if (best.isEmpty()) return CheckInResponse.builder()
            .message("No rooms available for type: " + req.getRequestedRoomType() +
                     ". Please try a different type or check back later.").build();

        Room room = best.get();
        room.setStatus(RoomStatus.OCCUPIED);
        room.setCurrentGuestName(req.getGuestName());
        roomRepo.save(room);

        Booking b = Booking.builder()
            .guestName(req.getGuestName()).roomNumber(room.getRoomNumber())
            .requestedType(req.getRequestedRoomType())
            .checkInDate(LocalDate.parse(req.getCheckInDate()))
            .status(BookingStatus.ACTIVE)
            .roomServiceCharges(0.0).additionalCharges(0.0)
            .discountRate(req.getDiscountPercent()/100.0).build();
        b = bookingRepo.save(b);
        room.setCurrentBookingId(b.getId());
        roomRepo.save(room);

        log.info("Guest {} checked in to room {}", req.getGuestName(), room.getRoomNumber());
        return CheckInResponse.builder()
            .bookingId(b.getId()).guestName(req.getGuestName())
            .assignedRoom(room.getRoomNumber()).floor(room.getFloor())
            .roomType(room.getRoomType().name()).nightlyRate(room.getNightlyRate())
            .checkInDate(req.getCheckInDate()).message("Check-in successful! Welcome to GrandStay Hotel.").build();
    }

    /** CHECK-OUT: calculates bill, marks room DIRTY, publishes room.vacated event. */
    @Transactional
    public CheckOutResponse checkOut(String roomNumber) {
        validateRoomNumber(roomNumber);
        Room room = roomRepo.findByRoomNumber(roomNumber)
            .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomNumber));
        Booking booking = bookingRepo.findByRoomNumberAndStatus(roomNumber, BookingStatus.ACTIVE)
            .orElseThrow(() -> new IllegalStateException("No active booking for room: " + roomNumber));

        CheckOutResponse bill = billingSvc.calculateBill(booking, room, billingSvc.isLateCheckout());

        booking.setCheckOutDate(LocalDate.now());
        booking.setStatus(BookingStatus.CHECKED_OUT);
        bookingRepo.save(booking);

        room.setStatus(RoomStatus.DIRTY);
        room.setCurrentGuestName(null);
        room.setCurrentBookingId(null);
        room.setCleanedAt(null);
        roomRepo.save(room);

        // EVENT-DRIVEN: publish to broker — Housekeeping subscribes automatically
        rabbit.convertAndSend(HotelOSEvents.EXCHANGE, HotelOSEvents.RK_VACATED,
            RoomVacatedEvent.builder().roomNumber(room.getRoomNumber()).floor(room.getFloor())
                .roomType(room.getRoomType().name()).vacatedAt(LocalDateTime.now())
                .formerGuestName(booking.getGuestName()).build());

        log.info("Room {} vacated, event published, bill=${}", roomNumber, bill.getTotalBill());
        return bill;
    }

    @Transactional
    public void addRoomServiceCharge(String roomNumber, double amount) {
        bookingRepo.findByRoomNumberAndStatus(roomNumber, BookingStatus.ACTIVE).ifPresent(b -> {
            b.setRoomServiceCharges(b.getRoomServiceCharges() + amount);
            bookingRepo.save(b);
            log.info("RS charge +${} on room {}", amount, roomNumber);
        });
    }

    public List<Room> getAllRooms() { return roomRepo.findAllByOrderByRoomNumber(); }

    private void validateRoomNumber(String roomNumber) {
        if (roomNumber == null || !roomNumber.matches("[0-9]{3}"))
            throw new IllegalArgumentException("Invalid room number format. Use 3 digits, e.g. 101");
    }
}
