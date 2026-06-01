package com.hotelos.reception.repository;
import com.hotelos.reception.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room,Long> {
    Optional<Room> findByRoomNumber(String roomNumber);
    @Query("SELECT r FROM Room r WHERE r.status=:status AND r.roomType=:type ORDER BY r.cleanedAt ASC NULLS LAST")
    List<Room> findEligible(@Param("status") RoomStatus status, @Param("type") RoomType type);
    List<Room> findAllByOrderByRoomNumber();
}
