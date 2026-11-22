package com.ject.exchange.api.dto.request;

import com.ject.exchange.api.dto.enums.ConfirmationAnswer;
import com.ject.exchange.api.dto.enums.NotMetReason;
import com.ject.exchange.application.dto.command.RespondConfirmationCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
class ConfirmationRequestTest {

    private static final Long APPOINTMENT_ID = 3L;
    private static final Long MEMBER_ID = 100L;

    @Test
    @DisplayName("DONE 요청은 약속 id · 회원 id · domain DONE · 이유 null인 커맨드가 된다")
    void toCommand_done_mapsToDomainDone() {
        RespondConfirmationCommand command = new ConfirmationRequest(ConfirmationAnswer.DONE, null, null)
                .toCommand(APPOINTMENT_ID, MEMBER_ID);

        assertEquals(new RespondConfirmationCommand(APPOINTMENT_ID, MEMBER_ID,
                com.ject.exchange.domain.domain.appointment.ConfirmationAnswer.DONE, null, null), command);
    }

    @ParameterizedTest
    @EnumSource(NotMetReason.class)
    @DisplayName("NOT_MET 요청은 같은 이름의 domain 이유와 직접 입력 그대로인 커맨드가 된다")
    void toCommand_notMet_mapsReasonByName(NotMetReason reason) {
        RespondConfirmationCommand command = new ConfirmationRequest(ConfirmationAnswer.NOT_MET, reason, "메모")
                .toCommand(APPOINTMENT_ID, MEMBER_ID);

        assertEquals(com.ject.exchange.domain.domain.appointment.ConfirmationAnswer.NOT_MET, command.answer());
        assertEquals(com.ject.exchange.domain.domain.appointment.NotMetReason.valueOf(reason.name()),
                command.notMetReason());
        assertEquals("메모", command.notMetDetail());
    }

    @Test
    @DisplayName("OTHER의 직접 입력 null은 null 그대로 간다")
    void toCommand_otherWithNullDetail_keepsNull() {
        RespondConfirmationCommand command = new ConfirmationRequest(ConfirmationAnswer.NOT_MET, NotMetReason.OTHER,
                null).toCommand(APPOINTMENT_ID, MEMBER_ID);

        assertNull(command.notMetDetail());
    }
}
