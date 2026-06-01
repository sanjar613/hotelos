package com.hotelos.roomservice;

import com.hotelos.roomservice.dto.OrderRequest;
import com.hotelos.roomservice.model.*;
import com.hotelos.roomservice.repository.OrderRepository;
import com.hotelos.roomservice.service.RoomOrderService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomOrderServiceTest {

    @Mock OrderRepository repo;
    @Mock RabbitTemplate rabbit;
    @Mock SimpMessagingTemplate ws;
    @InjectMocks RoomOrderService svc;

    private RoomOrder order(Long id, OrderStatus status) {
        return RoomOrder.builder().id(id).roomNumber("301")
            .items(List.of("Coffee x2","Sandwich")).totalAmount(18.50).status(status).build();
    }

    @Test @DisplayName("TS-04: place order creates RECEIVED status")
    void placeOrderCreatesReceived() {
        OrderRequest req = new OrderRequest();
        req.setRoomNumber("301"); req.setItems(List.of("Coffee x2","Sandwich")); req.setTotalAmount(18.50);
        when(repo.save(any())).thenAnswer(i -> { RoomOrder o = i.getArgument(0); o.setId(1L); return o; });

        RoomOrder result = svc.placeOrder(req);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.RECEIVED);
        assertThat(result.getRoomNumber()).isEqualTo("301");
        verify(ws).convertAndSend(eq("/topic/orders"), any()); // WebSocket push verified
    }

    @Test @DisplayName("TS-04: RECEIVED → PREPARING state advance")
    void advancesToPreparing() {
        when(repo.findById(1L)).thenReturn(Optional.of(order(1L, OrderStatus.RECEIVED)));
        when(repo.save(any())).thenAnswer(i->i.getArgument(0));

        RoomOrder result = svc.advance(1L);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.PREPARING);
    }

    @Test @DisplayName("TS-04: full state machine RECEIVED → DELIVERED")
    void fullStateMachine() {
        RoomOrder o = order(1L, OrderStatus.RECEIVED);
        when(repo.findById(1L)).thenReturn(Optional.of(o));
        when(repo.save(any())).thenAnswer(i->i.getArgument(0));

        // RECEIVED → PREPARING
        o.setStatus(OrderStatus.RECEIVED);
        RoomOrder r1 = svc.advance(1L); assertThat(r1.getStatus()).isEqualTo(OrderStatus.PREPARING);

        // PREPARING → OUT_FOR_DELIVERY
        o.setStatus(OrderStatus.PREPARING);
        RoomOrder r2 = svc.advance(1L); assertThat(r2.getStatus()).isEqualTo(OrderStatus.OUT_FOR_DELIVERY);

        // OUT_FOR_DELIVERY → DELIVERED (charge event published)
        o.setStatus(OrderStatus.OUT_FOR_DELIVERY);
        RoomOrder r3 = svc.advance(1L); assertThat(r3.getStatus()).isEqualTo(OrderStatus.DELIVERED);
        // On delivery, charge event published to broker
        verify(rabbit, atLeastOnce()).convertAndSend(any(String.class), eq("room.service.charge"), any(Object.class));
    }

    @Test @DisplayName("Already delivered order throws IllegalStateException")
    void advancingDeliveredThrows() {
        when(repo.findById(1L)).thenReturn(Optional.of(order(1L, OrderStatus.DELIVERED)));
        assertThatThrownBy(() -> svc.advance(1L)).isInstanceOf(IllegalStateException.class);
    }

    @Test @DisplayName("Order not found throws IllegalArgumentException")
    void notFoundThrows() {
        when(repo.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> svc.advance(99L)).isInstanceOf(IllegalArgumentException.class);
    }
}
