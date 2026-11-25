package com.ject.exchange.application.contract;

/**
 * 다른 BC가 회원의 현재 확정 약속 수를 조회하는 계약
 */
public interface ConfirmedAppointmentCountQuery {

    /**
     * 회원이 제안자 또는 받는 사람으로 참여한 현재 CONFIRMED 약속 수. memberId가 null이면 0
     */
    int countByMemberId(Long memberId);
}
