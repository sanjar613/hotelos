package com.hotelos.housekeeping.repository;
import com.hotelos.housekeeping.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface HkRoomRepository extends JpaRepository<HkRoom,String> {
    List<HkRoom> findByStatus(HkStatus status);
    List<HkRoom> findAllByOrderByRoomNumber();
}
