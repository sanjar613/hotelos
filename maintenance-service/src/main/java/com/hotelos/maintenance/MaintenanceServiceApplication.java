package com.hotelos.maintenance;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
/** Port 8084 — priority queue for maintenance requests: Critical > High > Normal > Low */
@SpringBootApplication
public class MaintenanceServiceApplication {
    public static void main(String[] args) { SpringApplication.run(MaintenanceServiceApplication.class, args); }
}
