package com.ject.notification.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 알림 처리(저장 · FCM 발송)를 알림을 발행한 요청 스레드와 떼어 돌린다.
 * FCM 응답을 기다리는 동안 다른 파트 API 응답이 늦어지지 않게 한다.
 */
@Configuration
@EnableAsync
public class NotificationAsyncConfig {

    /** NotificationRequestedListener의 @Async 값과 같아야 한다 */
    public static final String EXECUTOR = "notificationExecutor";

    /**
     * 대기열이 차면 버리지 않고 요청 스레드에서 바로 처리한다 (느려져도 알림은 남김).
     * 종료할 때는 대기 중인 알림을 마저 처리한다 (spring.lifecycle.timeout-per-shutdown-phase 30s 안).
     */
    @Bean(EXECUTOR)
    public ThreadPoolTaskExecutor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("notification-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(25);
        return executor;
    }
}
