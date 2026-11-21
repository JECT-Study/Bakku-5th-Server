package com.ject.user.domain.domain;

/**
 * 탈퇴 사유. 선택지만 있고 직접 입력은 없다 (G-09-2, 결정 A8).
 */
public enum WithdrawalReason {
    NO_WANTED_GOODS,
    HARD_TO_FIND_PARTNER,
    INCONVENIENT,
    OTHER
}
