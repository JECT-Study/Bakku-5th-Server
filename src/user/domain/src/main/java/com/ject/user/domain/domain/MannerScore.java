package com.ject.user.domain.domain;

/**
 * 매너 지표 (F-06-1, P-01-02). 시작 50, 보이는 범위 0~100.
 * 저장하는 값은 0~100으로 자르지 않는다. 자르면 되돌릴 때(교환 완료 되돌리기 · 노쇼 인정 취소) 원래 값으로 돌아가지 않는다.
 *
 * @param value 저장된 점수 (0~100 밖일 수 있음)
 */
public record MannerScore(int value) {

    public static final int START = 50;
    /** [교환했어요] 답으로 완료된 교환 1건 (이번 출시) */
    public static final int EXCHANGE_COMPLETED_POINTS = 2;

    private static final int MIN = 0;
    private static final int MAX = 100;

    public static MannerScore start() {
        return new MannerScore(START);
    }

    /**
     * 화면에 보이는 점수. 본인만 내 프로필에서 숫자를 본다 (G-03-1).
     */
    public int displayed() {
        return Math.clamp(value, MIN, MAX);
    }

    /**
     * 다른 사람에게는 숫자 대신 구간 이름만 보인다 (F-06-1, 11/24).
     */
    public MannerTier tier() {
        return MannerTier.of(displayed());
    }
}
