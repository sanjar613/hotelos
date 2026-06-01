package com.hotelos.reception.config;
import com.hotelos.reception.model.*;
import com.hotelos.reception.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.List;

/** Seeds 10 rooms across 2 floors. Skipped in test profile (uses H2). */
@Component @RequiredArgsConstructor @Slf4j @Profile("!test")
public class DataInitializer implements CommandLineRunner {
    private final RoomRepository repo;
    @Override public void run(String... args) {
        if (repo.count() > 0) { log.info("Rooms already seeded"); return; }
        repo.saveAll(List.of(
            room("101",1,RoomType.SINGLE,89.0, true,false,5),
            room("102",1,RoomType.DOUBLE,129.0,false,false,3),
            room("103",1,RoomType.DOUBLE,129.0,false,true, 7),
            room("104",1,RoomType.ACCESSIBLE,109.0,true,false,2),
            room("105",1,RoomType.SUITE,249.0,false,false,4),
            room("201",2,RoomType.SINGLE,89.0, true,false,6),
            room("202",2,RoomType.DOUBLE,139.0,false,false,1),
            room("203",2,RoomType.DOUBLE,139.0,false,true, 8),
            room("204",2,RoomType.SUITE,279.0, true,false,9),
            room("205",2,RoomType.ACCESSIBLE,119.0,true,false,3)
        ));
        log.info("Seeded 10 rooms across 2 floors");
    }
    private Room room(String num,int floor,RoomType type,double rate,boolean elev,boolean stairs,int hoursAgo) {
        return Room.builder().roomNumber(num).floor(floor).roomType(type).status(RoomStatus.CLEAN)
            .nightlyRate(rate).nearElevator(elev).nearStairs(stairs)
            .cleanedAt(LocalDateTime.now().minusHours(hoursAgo)).build();
    }
}
