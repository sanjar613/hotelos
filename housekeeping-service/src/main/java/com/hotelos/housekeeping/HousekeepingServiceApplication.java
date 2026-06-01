package com.hotelos.housekeeping;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
/** Port 8082 — manages room cleaning queue and status transitions */
@SpringBootApplication
public class HousekeepingServiceApplication {
    public static void main(String[] args) { SpringApplication.run(HousekeepingServiceApplication.class, args); }
}
