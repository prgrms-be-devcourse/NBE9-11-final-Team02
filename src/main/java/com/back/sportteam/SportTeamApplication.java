package com.back.sportteam;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class SportTeamApplication {

    public static void main(String[] args) {
        SpringApplication.run(SportTeamApplication.class, args);
    }

}
