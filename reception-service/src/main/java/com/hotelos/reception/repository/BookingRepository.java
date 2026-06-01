package com.hotelos.reception.repository;
import com.hotelos.reception.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking,Long> {
    Optional<Booking> findByRoomNumberAndStatus(String roomNumber, BookingStatus status);
    boolean existsByRoomNumberAndStatus(String roomNumber, BookingStatus status);
}
