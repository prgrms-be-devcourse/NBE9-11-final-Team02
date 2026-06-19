package com.back.sportteam.global.config;

import com.back.sportteam.global.util.TimeUtils;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeConfig {

    @Bean
    public Clock serviceClock() {
        return Clock.system(TimeUtils.SERVICE_ZONE);
    }
}
