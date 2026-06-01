package com.hotelos.reception.service;
import com.hotelos.reception.dto.CheckOutResponse;
import com.hotelos.reception.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.*;
import java.time.temporal.ChronoUnit;

@Service @Slf4j
public class BillingService {
    private static final double LATE_CHECKOUT_FEE = 50.0;

    public CheckOutResponse calculateBill(Booking booking, Room room, boolean lateCheckout) {
        // Step 1
        long nights = ChronoUnit.DAYS.between(booking.getCheckInDate(), LocalDate.now());
        if (nights < 1) { log.warn("Same-day checkout — charging min 1 night"); nights = 1; }

        // Step 2
        double roomSub = round(room.getNightlyRate() * nights);
        // Step 3
        double rsChrg  = round(booking.getRoomServiceCharges());
        // Step 4
        double addChrg = round(booking.getAdditionalCharges() + (lateCheckout ? LATE_CHECKOUT_FEE : 0));
        // Step 5
        double gross   = round(roomSub + rsChrg + addChrg);
        // Step 6
        double disc    = round(gross * booking.getDiscountRate());
        // Step 7
        double total   = round(Math.max(0, gross - disc));

        log.info("Bill booking={} nights={} room=${} rs=${} add=${} disc=${} TOTAL=${}", booking.getId(), nights, roomSub, rsChrg, addChrg, disc, total);
        return CheckOutResponse.builder()
            .bookingId(booking.getId()).guestName(booking.getGuestName())
            .roomNumber(booking.getRoomNumber()).numberOfNights((int)nights)
            .nightlyRate(room.getNightlyRate()).roomChargesSubtotal(roomSub)
            .roomServiceCharges(rsChrg).additionalCharges(addChrg)
            .discountAmount(disc).totalBill(total)
            .message("Checkout complete. Thank you for staying at GrandStay Hotel!").build();
    }

    public boolean isLateCheckout() { return LocalTime.now().isAfter(LocalTime.NOON); }
    private double round(double v) { return Math.round(v*100.0)/100.0; }
}
