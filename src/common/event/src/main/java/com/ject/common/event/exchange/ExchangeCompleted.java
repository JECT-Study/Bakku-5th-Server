package com.ject.common.event.exchange;

import java.util.List;

/**
 * 교환 완료. 회원은 양쪽 교환 완료 횟수를 1씩 올린다. 자동 완료도 포함한다 (F-01-2, G-04-1, 결정 B7).
 * 받는 쪽이 커밋 뒤에 처리하므로 교환 트랜잭션 안에서 발행한다.
 *
 * @param memberIds     교환한 두 회원 id
 * @param autoCompleted 자동 완료 여부 (2차 매너 지표에서 사용)
 */
public record ExchangeCompleted(Long appointmentId, List<Long> memberIds, boolean autoCompleted) {

    public ExchangeCompleted {
        memberIds = List.copyOf(memberIds);
    }
}
