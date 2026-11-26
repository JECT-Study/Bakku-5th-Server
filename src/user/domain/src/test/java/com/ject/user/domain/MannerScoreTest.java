package com.ject.user.domain;

import com.ject.user.domain.domain.MannerScore;
import com.ject.user.domain.domain.MannerTier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MannerScoreTest {

    @Test
    @DisplayName("누구나 50에서 시작하고, 새 회원은 굿즈헌터다")
    void start() {
        MannerScore score = MannerScore.start();

        assertEquals(50, score.displayed());
        assertEquals(MannerTier.GOODS_HUNTER, score.tier());
    }

    @ParameterizedTest(name = "{0}점 → 보이는 점수 {1}, {2}")
    @CsvSource({
            "100, 100, PRO_COLLECTOR",
            "80, 80, PRO_COLLECTOR",
            "79, 79, GOODS_HUNTER",
            "40, 40, GOODS_HUNTER",
            "39, 39, TRAINEE",
            "0, 0, TRAINEE",
            "104, 100, PRO_COLLECTOR",
            "-10, 0, TRAINEE"
    })
    @DisplayName("점수 구간별 이름: 80~100 프로컬렉터, 40~79 굿즈헌터, 0~39 교환연습생 (0~100 밖은 잘라서 본다)")
    void tier(int value, int displayed, MannerTier tier) {
        MannerScore score = new MannerScore(value);

        assertEquals(displayed, score.displayed());
        assertEquals(tier, score.tier());
    }

    @Test
    @DisplayName("저장 값은 자르지 않아서, 100에서 올렸다가 되돌리면 다시 100이다")
    void reversibleAboveMax() {
        MannerScore raised = new MannerScore(100 + MannerScore.EXCHANGE_COMPLETED_POINTS);
        MannerScore reverted = new MannerScore(raised.value() - MannerScore.EXCHANGE_COMPLETED_POINTS);

        assertEquals(100, raised.displayed());
        assertEquals(100, reverted.displayed());
    }
}
