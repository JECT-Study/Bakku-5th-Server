package com.ject.user.application.port;

import com.ject.user.domain.domain.WithdrawalReason;

import java.time.Instant;

/**
 * 탈퇴 사유 저장소 출력 포트. 사유는 서비스 개선용 통계라 회원 정보와 따로 둔다.
 */
public interface WithdrawalReasonRepository {

    void save(Long memberId, WithdrawalReason reason, Instant withdrawnAt);
}
