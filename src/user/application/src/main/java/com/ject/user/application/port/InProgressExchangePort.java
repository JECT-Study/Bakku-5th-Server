package com.ject.user.application.port;

/**
 * 진행 중인 교환 수 조회 출력 포트. 구현은 교환 파트 조회를 부르는 infrastructure Adapter가 담당한다.
 * 탈퇴 전 안내(MB-5) "진행 중인 교환이 n건 있어요"에 쓴다 (G-09-2). 탈퇴는 막지 않는다 (결정 B5).
 */
public interface InProgressExchangePort {

    /**
     * @return 이 회원이 참여한 확정 약속(CONFIRMED) 수
     */
    int countInProgress(Long memberId);
}
