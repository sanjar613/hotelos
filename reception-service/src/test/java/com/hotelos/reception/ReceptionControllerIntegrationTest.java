package com.hotelos.reception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotelos.reception.dto.CheckInRequest;
import com.hotelos.reception.model.*;
import com.hotelos.reception.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests — H2 in-memory DB, RabbitMQ and WebSocket are mocked.
 * Tests full HTTP → Service → Repository stack.
 */
@SpringBootTest
@ActiveProfiles("test")
class ReceptionControllerIntegrationTest {

    @Autowired WebApplicationContext ctx;
    @Autowired RoomRepository roomRepo;
    @Autowired BookingRepository bookingRepo;
    @Autowired ObjectMapper objectMapper;

    // Mock broker — tests don't need real RabbitMQ running
    @MockBean RabbitTemplate rabbitTemplate;
    @MockBean SimpMessagingTemplate simpMessagingTemplate;

    MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(ctx).apply(springSecurity()).build();
        bookingRepo.deleteAll();
        roomRepo.deleteAll();
        seedRoom("201", 2, RoomType.DOUBLE, 139.0, true);
        seedRoom("202", 2, RoomType.DOUBLE, 139.0, false);
        seedRoom("105", 1, RoomType.SUITE, 249.0, false);
    }

    private void seedRoom(String num, int floor, RoomType type, double rate, boolean elev) {
        roomRepo.save(Room.builder()
            .roomNumber(num).floor(floor).roomType(type)
            .status(RoomStatus.CLEAN).nightlyRate(rate)
            .cleanedAt(LocalDateTime.now().minusHours(5))
            .nearElevator(elev).nearStairs(false).build());
    }

    @Test @DisplayName("TS-01: Check-in assigns available double room")
    void checkInAssignsRoom() throws Exception {
        CheckInRequest req = new CheckInRequest();
        req.setGuestName("Alice Smith");
        req.setRequestedRoomType(RoomType.DOUBLE);
        req.setCheckInDate(LocalDate.now().toString());

        mockMvc.perform(post("/api/reception/checkin")
                .with(httpBasic("admin","hotelos2024"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.assignedRoom").isNotEmpty())
            .andExpect(jsonPath("$.bookingId").isNumber());
    }

    @Test @DisplayName("TS-07: No suite rooms — returns informative message, no crash")
    void noRoomsReturnsMessage() throws Exception {
        CheckInRequest req = new CheckInRequest();
        req.setGuestName("Bob Jones");
        req.setRequestedRoomType(RoomType.ACCESSIBLE); // none seeded
        req.setCheckInDate(LocalDate.now().toString());

        mockMvc.perform(post("/api/reception/checkin")
                .with(httpBasic("admin","hotelos2024"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("No rooms available")));
    }

    @Test @DisplayName("TS-08: Invalid room number format returns 400")
    void invalidRoomNumberReturns400() throws Exception {
        mockMvc.perform(post("/api/reception/checkout/abc")
                .with(httpBasic("admin","hotelos2024")))
            .andExpect(status().isBadRequest());
    }

    @Test @DisplayName("Unauthenticated request returns 401")
    void unauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/reception/rooms"))
            .andExpect(status().isUnauthorized());
    }

    @Test @DisplayName("Validation: missing guest name returns 400 with field error")
    void missingGuestNameReturns400() throws Exception {
        CheckInRequest req = new CheckInRequest();
        req.setRequestedRoomType(RoomType.DOUBLE);
        req.setCheckInDate(LocalDate.now().toString());

        mockMvc.perform(post("/api/reception/checkin")
                .with(httpBasic("admin","hotelos2024"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.guestName").exists());
    }

    @Test @DisplayName("TS-06: Two check-ins get different rooms (no double-booking)")
    void twoCheckInsGetDifferentRooms() throws Exception {
        CheckInRequest req1 = new CheckInRequest();
        req1.setGuestName("Alice"); req1.setRequestedRoomType(RoomType.DOUBLE);
        req1.setCheckInDate(LocalDate.now().toString());

        CheckInRequest req2 = new CheckInRequest();
        req2.setGuestName("Bob"); req2.setRequestedRoomType(RoomType.DOUBLE);
        req2.setCheckInDate(LocalDate.now().toString());

        String r1 = mockMvc.perform(post("/api/reception/checkin")
                .with(httpBasic("admin","hotelos2024"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req1)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        String r2 = mockMvc.perform(post("/api/reception/checkin")
                .with(httpBasic("admin","hotelos2024"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req2)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        String room1 = objectMapper.readTree(r1).get("assignedRoom").asText();
        String room2 = objectMapper.readTree(r2).get("assignedRoom").asText();
        org.assertj.core.api.Assertions.assertThat(room1).isNotEqualTo(room2);
    }
}
