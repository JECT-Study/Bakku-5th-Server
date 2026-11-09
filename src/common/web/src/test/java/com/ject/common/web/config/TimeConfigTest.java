package com.ject.common.web.config;

import com.ject.common.core.time.ServiceTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

import java.time.Clock;
import java.time.ZoneId;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TimeConfigTest {

    @Test
    @DisplayName("app.time-zone 설정값으로 Clock과 ServiceTime을 등록한다")
    void registersClockWithConfiguredZone() {
        // given & when
        try (AnnotationConfigApplicationContext context = contextWith("Asia/Seoul")) {
            context.refresh();

            // then
            assertEquals(ZoneId.of("Asia/Seoul"), context.getBean(Clock.class).getZone());
            assertNotNull(context.getBean(ServiceTime.class));
        }
    }

    @Test
    @DisplayName("잘못된 시간대면 앱 기동이 실패한다")
    void failsOnInvalidZone() {
        // given
        try (AnnotationConfigApplicationContext context = contextWith("Not/AZone")) {
            // when & then
            assertThrows(Exception.class, context::refresh);
        }
    }

    private static AnnotationConfigApplicationContext contextWith(String zone) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources()
                .addFirst(new MapPropertySource("test", Map.of("app.time-zone", zone)));
        context.register(TimeConfig.class);
        return context;
    }
}
