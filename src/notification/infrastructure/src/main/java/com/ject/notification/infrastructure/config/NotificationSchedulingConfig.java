package com.ject.notification.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 예약 알림 1분 배치를 켠다 (결정 C4).
 */
@Configuration
@EnableScheduling
public class NotificationSchedulingConfig {
}
