package com.ject.common.event.exchange;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("139-withdrawn-appointment-cancel-event")
@Tag("AC:139-withdrawn-appointment-cancel-event:AC-5")
class AppointmentCanceledByMemberWithdrawalTest {

    @Test
    @DisplayName("발행 뒤 원본 글 ID 목록이 바뀌어도 이벤트 값은 바뀌지 않는다")
    void constructor_copiesPostIds() {
        List<Long> source = new ArrayList<>(List.of(10L, 20L));
        AppointmentCanceledByMemberWithdrawal event =
                new AppointmentCanceledByMemberWithdrawal(1L, source, 100L);

        source.add(30L);

        assertEquals(List.of(10L, 20L), event.postIds());
    }

    @Test
    @DisplayName("이벤트가 반환한 글 ID 목록은 수정할 수 없다")
    void postIds_areUnmodifiable() {
        AppointmentCanceledByMemberWithdrawal event =
                new AppointmentCanceledByMemberWithdrawal(1L, List.of(10L, 20L), 100L);

        assertThrows(UnsupportedOperationException.class, () -> event.postIds().add(30L));
    }
}
