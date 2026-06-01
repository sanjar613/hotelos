package com.hotelos.reception;

import com.hotelos.reception.dto.CheckOutResponse;
import com.hotelos.reception.model.*;
import com.hotelos.reception.service.BillingService;
import org.junit.jupiter.api.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for the Billing Calculation Algorithm.
 * Covers all edge cases: same-day, late checkout, discount, zero charges.
 */
class BillingServiceTest {

    BillingService billingService = new BillingService();

    private Booking booking(double rsChrg, double addChrg, double disc, LocalDate checkIn) {
        Booking b = new Booking();
        b.setId(1L); b.setGuestName("Test Guest");
        b.setRoomNumber("101"); b.setCheckInDate(checkIn);
        b.setRoomServiceCharges(rsChrg);
        b.setAdditionalCharges(addChrg);
        b.setDiscountRate(disc);
        b.setStatus(BookingStatus.ACTIVE);
        return b;
    }

    private Room room(double rate) {
        Room r = new Room(); r.setNightlyRate(rate); r.setRoomNumber("101"); return r;
    }

    @Test @DisplayName("Standard 3-night stay — correct total")
    void standardStay() {
        LocalDate checkIn = LocalDate.now().minusDays(3);
        CheckOutResponse bill = billingService.calculateBill(booking(25.0, 0.0, 0.0, checkIn), room(100.0), false);

        assertThat(bill.getNumberOfNights()).isEqualTo(3);
        assertThat(bill.getRoomChargesSubtotal()).isEqualTo(300.0);
        assertThat(bill.getRoomServiceCharges()).isEqualTo(25.0);
        assertThat(bill.getTotalBill()).isEqualTo(325.0);
    }

    @Test @DisplayName("Edge case: same-day checkout charges minimum 1 night")
    void sameDayChargesOneNight() {
        CheckOutResponse bill = billingService.calculateBill(booking(0.0, 0.0, 0.0, LocalDate.now()), room(150.0), false);

        assertThat(bill.getNumberOfNights()).isEqualTo(1);
        assertThat(bill.getRoomChargesSubtotal()).isEqualTo(150.0);
    }

    @Test @DisplayName("Late checkout adds $50 fee")
    void lateCheckoutFee() {
        CheckOutResponse bill = billingService.calculateBill(booking(0.0, 0.0, 0.0, LocalDate.now().minusDays(1)), room(100.0), true);

        assertThat(bill.getAdditionalCharges()).isEqualTo(50.0);
        assertThat(bill.getTotalBill()).isEqualTo(150.0);
    }

    @Test @DisplayName("Discount applied correctly — 20% off")
    void discountApplied() {
        LocalDate checkIn = LocalDate.now().minusDays(2);
        CheckOutResponse bill = billingService.calculateBill(booking(0.0, 0.0, 0.20, checkIn), room(100.0), false);

        assertThat(bill.getDiscountAmount()).isEqualTo(40.0);   // 200 * 0.20
        assertThat(bill.getTotalBill()).isEqualTo(160.0);
    }

    @Test @DisplayName("Zero charges — guest with no extras")
    void zeroCharges() {
        CheckOutResponse bill = billingService.calculateBill(booking(0.0, 0.0, 0.0, LocalDate.now().minusDays(1)), room(100.0), false);

        assertThat(bill.getRoomServiceCharges()).isEqualTo(0.0);
        assertThat(bill.getAdditionalCharges()).isEqualTo(0.0);
        assertThat(bill.getTotalBill()).isEqualTo(100.0);
    }

    @Test @DisplayName("Total bill never goes negative")
    void totalNeverNegative() {
        // 100% discount
        CheckOutResponse bill = billingService.calculateBill(booking(0.0, 0.0, 1.0, LocalDate.now().minusDays(1)), room(100.0), false);

        assertThat(bill.getTotalBill()).isGreaterThanOrEqualTo(0.0);
    }
}
