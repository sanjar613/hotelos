package com.hotelos.maintenance.repository;
import com.hotelos.maintenance.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MaintenanceRepository extends JpaRepository<MaintenanceIssue,Long> {

    /**
     * PRIORITY QUEUE ALGORITHM query:
     * Returns OPEN/ASSIGNED issues sorted by urgency ASC (CRITICAL first)
     * then createdAt ASC (FIFO within same urgency level).
     * This implements: "if two requests have same urgency, submitted-first takes priority."
     */
    @Query("SELECT m FROM MaintenanceIssue m WHERE m.status IN ('OPEN','ASSIGNED') " +
           "ORDER BY m.urgency ASC, m.createdAt ASC")
    List<MaintenanceIssue> findQueueOrdered();

    List<MaintenanceIssue> findAllByOrderByUrgencyAscCreatedAtAsc();
}
