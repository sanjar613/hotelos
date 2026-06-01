package com.hotelos.roomservice.service;
import com.hotelos.roomservice.config.HotelOSEvents;
import com.hotelos.roomservice.dto.OrderRequest;
import com.hotelos.roomservice.event.OrderStatusChangedEvent;
import com.hotelos.roomservice.model.*;
import com.hotelos.roomservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service @RequiredArgsConstructor @Slf4j
public class RoomOrderService {

    private final OrderRepository repo;
    private final RabbitTemplate rabbit;
    private final SimpMessagingTemplate ws;

    /** TS-04: place order — starts as RECEIVED */
    @Transactional
    public RoomOrder placeOrder(OrderRequest req) {
        RoomOrder order = RoomOrder.builder()
            .roomNumber(req.getRoomNumber())
            .items(req.getItems())
            .totalAmount(req.getTotalAmount())
            .status(OrderStatus.RECEIVED).build();
        order = repo.save(order);
        publishAndPush(order, null, OrderStatus.RECEIVED);
        log.info("Order #{} placed for room {} total=${}", order.getId(), order.getRoomNumber(), order.getTotalAmount());
        return order;
    }

    /** TS-04: advance order through state machine — each step triggers WebSocket push */
    @Transactional
    public RoomOrder advance(Long orderId) {
        RoomOrder order = repo.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        if (order.getStatus() == OrderStatus.DELIVERED)
            throw new IllegalStateException("Order " + orderId + " already delivered");
        OrderStatus old = order.getStatus();
        OrderStatus next = nextStatus(old);
        order.setStatus(next);
        order = repo.save(order);
        publishAndPush(order, old, next);
        if (next == OrderStatus.DELIVERED) publishCharge(order);
        return order;
    }

    public List<RoomOrder> getActive() {
        return repo.findByStatusNotOrderByCreatedAtAsc(OrderStatus.DELIVERED);
    }

    public List<RoomOrder> getByRoom(String room) {
        return repo.findByRoomNumberOrderByCreatedAtDesc(room);
    }

    private OrderStatus nextStatus(OrderStatus s) {
        return switch (s) {
            case RECEIVED         -> OrderStatus.PREPARING;
            case PREPARING        -> OrderStatus.OUT_FOR_DELIVERY;
            case OUT_FOR_DELIVERY -> OrderStatus.DELIVERED;
            case DELIVERED        -> throw new IllegalStateException("Already delivered");
        };
    }

    private void publishAndPush(RoomOrder order, OrderStatus old, OrderStatus next) {
        OrderStatusChangedEvent ev = OrderStatusChangedEvent.builder()
            .orderId(order.getId()).roomNumber(order.getRoomNumber())
            .oldStatus(old!=null?old.name():null).newStatus(next.name())
            .items(order.getItems()).totalAmount(order.getTotalAmount())
            .changedAt(LocalDateTime.now()).build();
        rabbit.convertAndSend(HotelOSEvents.EXCHANGE, HotelOSEvents.RK_ORDER, ev);
        ws.convertAndSend("/topic/orders", ev);
        log.debug("Order #{} {} -> {}", order.getId(), old, next);
    }

    private void publishCharge(RoomOrder order) {
        rabbit.convertAndSend(HotelOSEvents.EXCHANGE, HotelOSEvents.RK_CHARGE,
            Map.of("roomNumber", order.getRoomNumber(), "amount", order.getTotalAmount(), "orderId", order.getId()));
        log.info("Charge ${}  published for room {}", order.getTotalAmount(), order.getRoomNumber());
    }
}
