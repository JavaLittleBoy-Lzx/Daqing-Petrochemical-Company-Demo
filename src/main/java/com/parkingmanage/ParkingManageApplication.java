package com.parkingmanage;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling  // 启用定时任务支持
public class ParkingManageApplication {
    public static void main(String[] args) {
        SpringApplication.run(ParkingManageApplication.class, args);
    }
} 