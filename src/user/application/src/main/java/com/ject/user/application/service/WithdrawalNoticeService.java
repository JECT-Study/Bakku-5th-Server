package com.ject.user.application.service;

import com.ject.user.application.port.InProgressExchangePort;
import org.springframework.stereotype.Service;

/**
 * 탈퇴 전 안내 (MB-5). 진행 중인 교환이 있어도 탈퇴는 막지 않고 안내만 한다 (G-09-2, 결정 B5).
 */
@Service
public class WithdrawalNoticeService {

    private final InProgressExchangePort inProgressExchangePort;

    public WithdrawalNoticeService(InProgressExchangePort inProgressExchangePort) {
        this.inProgressExchangePort = inProgressExchangePort;
    }

    /**
     * @return 이 회원이 참여한 확정 약속 수. 0이면 FE가 안내 문구를 보여주지 않는다
     */
    public int countInProgressExchanges(Long memberId) {
        return inProgressExchangePort.countInProgress(memberId);
    }
}
