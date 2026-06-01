package com.hotelos.roomservice.repository;
import com.hotelos.roomservice.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<RoomOrder,Long> {
    List<RoomOrder> findByRoomNumberOrderByCreatedAtDesc(String roomNumber);
    List<RoomOrder> findByStatusNotOrderByCreatedAtAsc(OrderStatus status);
}
