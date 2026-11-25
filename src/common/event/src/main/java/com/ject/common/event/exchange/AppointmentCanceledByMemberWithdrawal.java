package com.ject.common.event.exchange;

import java.util.List;

/**
 * 회원 탈퇴로 확정 약속이 취소됨. post BC가 각 글의 작성자를 조회해 탈퇴 회원 글은 삭제하고 상대 글은 교환 가능으로 되돌린다 (#139).
 * exchange 트랜잭션에서 커밋 뒤에 처리하도록 트랜잭션 안에서 발행한다.
 *
 * @param postIds           교환 대상 글과 그 약속에서 고른 첨부 글 (중복 없음, 순서는 계약으로 보장하지 않음)
 * @param withdrawnMemberId 탈퇴한 회원 id
 */
public record AppointmentCanceledByMemberWithdrawal(Long appointmentId, List<Long> postIds, Long withdrawnMemberId) {

    public AppointmentCanceledByMemberWithdrawal {
        postIds = List.copyOf(postIds);
    }
}
