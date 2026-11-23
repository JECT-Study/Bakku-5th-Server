package com.ject.notification.application.port;

import com.ject.notification.domain.domain.ScheduledNotification;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface ScheduledNotificationRepository {

    /**
     * 같은 예약 키 · 받는 사람이 이미 있으면 새 시각 · 내용으로 바꾸고, 없으면 추가한다.
     */
    void saveAll(List<ScheduledNotification> notifications);

    void deleteByScheduleKey(String scheduleKey);

    /**
     * 보낼 시각이 지난 예약을 잠가서 가져온다. 다른 서버가 이미 잡은 건 건너뛰어 한 번만 보낸다 (결정 C4).
     */
    List<ScheduledNotification> findDueForUpdate(Instant now, int limit);

    void deleteAllById(Collection<Long> ids);
}
