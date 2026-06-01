package com.hotelos.maintenance;

import com.hotelos.maintenance.dto.IssueRequest;
import com.hotelos.maintenance.model.*;
import com.hotelos.maintenance.repository.MaintenanceRepository;
import com.hotelos.maintenance.service.MaintenanceService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MaintenanceServiceTest {

    @Mock MaintenanceRepository repo;
    @Mock RabbitTemplate rabbit;
    @Mock SimpMessagingTemplate ws;
    @InjectMocks MaintenanceService svc;

    private IssueRequest req(String room, UrgencyLevel urgency, String desc) {
        IssueRequest r = new IssueRequest();
        r.setRoomNumber(room); r.setUrgency(urgency); r.setDescription(desc); return r;
    }

    @Test @DisplayName("TS-05: CRITICAL issue is saved and assigned to technician")
    void criticalIssueAssigned() {
        when(repo.save(any())).thenAnswer(i -> { MaintenanceIssue m=i.getArgument(0); m.setId(1L); return m; });

        MaintenanceIssue result = svc.reportIssue(req("115", UrgencyLevel.CRITICAL, "Broken shower"));

        assertThat(result.getUrgency()).isEqualTo(UrgencyLevel.CRITICAL);
        assertThat(result.getStatus()).isEqualTo(IssueStatus.ASSIGNED);
        assertThat(result.getAssignedTechnician()).isNotBlank();
        verify(ws).convertAndSend(eq("/topic/maintenance"), any());
    }

    @Test @DisplayName("Priority queue: CRITICAL has lower ordinal than HIGH (CRITICAL first)")
    void criticalBeforeHighByOrdinal() {
        // UrgencyLevel enum: CRITICAL=0, HIGH=1, NORMAL=2, LOW=3
        assertThat(UrgencyLevel.CRITICAL.ordinal()).isLessThan(UrgencyLevel.HIGH.ordinal());
        assertThat(UrgencyLevel.HIGH.ordinal()).isLessThan(UrgencyLevel.NORMAL.ordinal());
        assertThat(UrgencyLevel.NORMAL.ordinal()).isLessThan(UrgencyLevel.LOW.ordinal());
    }

    @Test @DisplayName("Resolve issue changes status to RESOLVED")
    void resolveIssue() {
        MaintenanceIssue issue = MaintenanceIssue.builder()
            .id(1L).roomNumber("115").description("Shower").urgency(UrgencyLevel.CRITICAL)
            .status(IssueStatus.ASSIGNED).assignedTechnician("Tom").createdAt(LocalDateTime.now()).build();
        when(repo.findById(1L)).thenReturn(Optional.of(issue));
        when(repo.save(any())).thenAnswer(i->i.getArgument(0));

        MaintenanceIssue result = svc.resolveIssue(1L);

        assertThat(result.getStatus()).isEqualTo(IssueStatus.RESOLVED);
        assertThat(result.getResolvedAt()).isNotNull();
        verify(rabbit).convertAndSend(any(String.class), contains("resolved"), any());
    }

    @Test @DisplayName("Resolving already-resolved issue throws IllegalStateException")
    void resolvingResolvedThrows() {
        MaintenanceIssue issue = MaintenanceIssue.builder()
            .id(1L).status(IssueStatus.RESOLVED).roomNumber("101").description("x")
            .urgency(UrgencyLevel.LOW).createdAt(LocalDateTime.now()).build();
        when(repo.findById(1L)).thenReturn(Optional.of(issue));

        assertThatThrownBy(() -> svc.resolveIssue(1L)).isInstanceOf(IllegalStateException.class);
    }

    @Test @DisplayName("Technicians assigned round-robin")
    void roundRobinAssignment() {
        when(repo.save(any())).thenAnswer(i -> { MaintenanceIssue m=i.getArgument(0); m.setId((long)(Math.random()*100)); return m; });

        Set<String> assigned = new HashSet<>();
        for (int i = 0; i < 4; i++) {
            MaintenanceIssue m = svc.reportIssue(req("10"+i, UrgencyLevel.LOW, "Issue "+i));
            assigned.add(m.getAssignedTechnician());
        }
        assertThat(assigned).hasSizeGreaterThan(1); // Multiple different technicians assigned
    }
}
