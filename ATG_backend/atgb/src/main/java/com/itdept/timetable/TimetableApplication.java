package com.itdept.timetable;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TimetableApplication {
    public static void main(String[] args) {
        SpringApplication.run(TimetableApplication.class, args);
        System.out.println("compile successfully");
    }
}