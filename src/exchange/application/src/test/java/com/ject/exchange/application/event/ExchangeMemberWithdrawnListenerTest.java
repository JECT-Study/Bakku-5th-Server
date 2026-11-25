package com.ject.exchange.application.event;

import com.ject.common.event.member.MemberWithdrawn;
import com.ject.exchange.application.service.command.WithdrawnMemberAppointmentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("139-withdrawn-appointment-cancel-event")
class ExchangeMemberWithdrawnListenerTest {

    private static final Long MEMBER_ID = 100L;
    private static final Instant WITHDRAWN_AT = Instant.parse("2026-11-25T03:00:00Z");

    @Test
    @Tag("AC:139-withdrawn-appointment-cancel-event:AC-1")
    @DisplayName("회원 탈퇴 이벤트 값을 약속 취소 서비스에 그대로 전달한다")
    void handle_delegatesEventValues() {
        CapturingService service = new CapturingService();
        ExchangeMemberWithdrawnListener listener = new ExchangeMemberWithdrawnListener(service);

        listener.handle(new MemberWithdrawn(MEMBER_ID, WITHDRAWN_AT));

        assertEquals(MEMBER_ID, service.memberId);
        assertEquals(WITHDRAWN_AT, service.withdrawnAt);
    }

    @Test
    @Tag("AC:139-withdrawn-appointment-cancel-event:AC-9")
    @DisplayName("회원 탈퇴 커밋 뒤 약속 취소가 실패해도 예외를 외부로 전파하지 않는다")
    void handle_serviceFailure_doesNotPropagate() {
        WithdrawnMemberAppointmentService failingService = new WithdrawnMemberAppointmentService(null, null, null) {
            @Override
            public void cancelActiveAppointments(Long memberId, Instant withdrawnAt) {
                throw new IllegalStateException("약속 취소 실패");
            }
        };
        ExchangeMemberWithdrawnListener listener = new ExchangeMemberWithdrawnListener(failingService);

        assertDoesNotThrow(() -> listener.handle(new MemberWithdrawn(MEMBER_ID, WITHDRAWN_AT)));
    }

    private static final class CapturingService extends WithdrawnMemberAppointmentService {

        private Long memberId;
        private Instant withdrawnAt;

        private CapturingService() {
            super(null, null, null);
        }

        @Override
        public void cancelActiveAppointments(Long memberId, Instant withdrawnAt) {
            this.memberId = memberId;
            this.withdrawnAt = withdrawnAt;
        }
    }
}
