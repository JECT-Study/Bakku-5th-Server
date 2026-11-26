package com.ject.user.domain.domain;

/**
 * 매너 지표 구간 이름 (F-06-1, P-01-02, 11/24). 누구나 50에서 시작해 새 회원은 굿즈헌터다.
 */
public enum MannerTier {
    /** 80~100 */
    PRO_COLLECTOR(80),
    /** 40~79 */
    GOODS_HUNTER(40),
    /** 0~39 */
    TRAINEE(0);

    private final int minScore;

    MannerTier(int minScore) {
        this.minScore = minScore;
    }

    /**
     * @param displayedScore 0~100으로 자른 점수
     */
    public static MannerTier of(int displayedScore) {
        for (MannerTier tier : values()) {
            if (displayedScore >= tier.minScore) {
                return tier;
            }
        }
        return TRAINEE;
    }
}
