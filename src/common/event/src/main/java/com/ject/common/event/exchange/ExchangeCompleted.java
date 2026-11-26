package com.ject.common.event.exchange;

import java.util.List;

/**
 * 교환 완료. 회원은 양쪽 교환 완료 횟수를 1씩 올린다. 자동 완료도 포함한다 (F-01-2, G-04-1, 결정 B7).
 * 받는 쪽이 커밋 뒤에 처리하므로 교환 트랜잭션 안에서 발행한다.
 *
 * @param memberIds     교환한 두 회원 id
 * @param autoCompleted [교환했어요] 답이 아닌 완료면 true. 매너 지표 +2는 false(교환했어요 답)일 때만 준다 (F-06-1).
 *                      자동 완료 · 교환 확인 질문 뒤 나가기(E-09-1) · 교환 완료로 바꾸기(G-02-3)는 true로 보낸다.
 *                      "앱 밖에서 교환했어요"는 완료 횟수도 올리지 않으므로 발행하지 않는다
 */
public record ExchangeCompleted(Long appointmentId, List<Long> memberIds, boolean autoCompleted) {

    public ExchangeCompleted {
        memberIds = List.copyOf(memberIds);
    }
}
