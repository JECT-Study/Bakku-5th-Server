package com.ject.user.application.port;

import com.ject.user.domain.domain.Report;

import java.util.Optional;

/**
 * 신고 저장소 출력 포트. 구현은 infrastructure가 담당한다.
 */
public interface ReportRepository {

    /**
     * 같은 신고자가 같은 대상을 신고한 적이 없을 때만 저장한다. 같은 요청이 동시에 와도 한 행만 남는다 (결정 C8).
     *
     * @return 새 신고 id, 이미 신고했으면 empty
     */
    Optional<Long> saveIfAbsent(Report report);
}
