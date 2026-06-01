package com.hotelos.roomservice;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
/** Port 8083 — food/drink orders, state machine RECEIVED→PREPARING→OUT_FOR_DELIVERY→DELIVERED */
@SpringBootApplication
public class RoomServiceApplication {
    public static void main(String[] args) { SpringApplication.run(RoomServiceApplication.class, args); }
}
