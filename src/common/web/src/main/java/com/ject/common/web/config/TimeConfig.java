package com.ject.common.web.config;

import com.ject.common.core.time.ServiceTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class TimeConfig {

    @Bean
    public Clock clock(@Value("${app.time-zone}") ZoneId zone) {
        return Clock.system(zone);
    }

    @Bean
    public ServiceTime serviceTime(Clock clock) {
        return new ServiceTime(clock);
    }
}
