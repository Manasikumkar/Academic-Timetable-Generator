package com.itdept.timetable.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "ga")
@Data
public class GaProperties {
    private int populationSize = 100;
    private int maxGenerations = 500;
    private double mutationRate = 0.05;
    private double crossoverRate = 0.80;
    private int eliteCount = 5;
    private int tournamentSize = 5;
}