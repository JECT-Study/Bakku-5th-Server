package com.ject.exchange.domain.domain.appointment;

import com.ject.exchange.domain.domain.appointment.vo.AppointmentItemIds;
import com.ject.exchange.domain.domain.appointment.vo.AppointmentTime;
import com.ject.exchange.domain.domain.appointment.vo.ConfirmationReply;
import com.ject.exchange.domain.domain.appointment.vo.Place;
import com.ject.exchange.domain.exception.ExchangeErrorCode;
import com.ject.exchange.domain.exception.ExchangeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
class AppointmentConfirmationTest {

    /** 2026-11-23 15:00 (+09:00) 약속 시각 */
    private static final Instant SCHEDULED_AT = Instant.parse("2026-11-23T06:00:00Z");
    /** 2026-11-23 15:30 (+09:00) 교환 확인 열림 시각 */
    private static final Instant OPENS_AT = SCHEDULED_AT.plus(Duration.ofMinutes(30));
    /** 2026-11-22 17:00 (+09:00) 확정 시각 */
    private static final Instant CONFIRMED_AT = Instant.parse("2026-11-22T08:00:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2026-11-22T07:00:00Z");
    private static final Instant CREATED_AT = Instant.parse("2026-11-22T06:00:00Z");
    /** 이유 남기기 기한 = 열림 시각 + 3일 */
    private static final Instant REASON_DEADLINE = OPENS_AT.plus(Duration.ofDays(3));
    private static final Long APPOINTMENT_ID = 3L;
    private static final Long EXCHANGE_ID = 1L;
    /** 제안자 A */
    private static final Long REQUESTER_ID = 100L;
    /** 받는 사람 B */
    private static final Long OWNER_ID = 200L;
    /** 참여자가 아닌 C */
    private static final Long OTHER_MEMBER_ID = 300L;
    private static final Place PLACE = new Place("홍대입구역 3번 출구 앞 편의점", "서울 마포구 양화로 160", 37.5571, 126.9245);

    // ---- AC-1 먼저 교환했어요 ----

    @Test
    @DisplayName("제안자가 열림 시각 정각에 DONE으로 답하면 COMPLETED · 결과 시각 now · 응답 1건이다")
    void answerConfirmation_doneByProposerAtOpen_completes() {
        // given
        Appointment confirmed = confirmed();

        // when
        Appointment answered = confirmed.answerConfirmation(REQUESTER_ID, ConfirmationAnswer.DONE, null, null, OPENS_AT);

        // then
        assertEquals(AppointmentStatus.COMPLETED, answered.getStatus());
        assertEquals(OPENS_AT, answered.getResolvedAt());
        assertEquals(Optional.of(new ConfirmationReply(REQUESTER_ID, ConfirmationAnswer.DONE, null, null, OPENS_AT)),
                answered.replyOf(REQUESTER_ID));
        assertTrue(answered.replyOf(OWNER_ID).isEmpty());
    }

    @Test
    @DisplayName("DONE으로 답해도 시간 · 장소 · 아이템 · 알림 · 확정 시각은 그대로다")
    void answerConfirmation_done_keepsOtherFields() {
        Appointment confirmed = confirmed();

        Appointment answered = confirmed.answerConfirmation(REQUESTER_ID, ConfirmationAnswer.DONE, null, null, OPENS_AT);

        assertEquals(APPOINTMENT_ID, answered.getId());
        assertEquals(EXCHANGE_ID, answered.getExchangeId());
        assertEquals(REQUESTER_ID, answered.getProposerUserId());
        assertEquals(OWNER_ID, answered.getReceiverUserId());
        assertEquals(confirmed.getTime(), answered.getTime());
        assertEquals(PLACE, answered.getPlace());
        assertEquals(List.of(31L), answered.getItemIds().values());
        assertEquals(30, answered.getProposerReminderOffsetMinutes());
        assertEquals(60, answered.getReceiverReminderOffsetMinutes());
        assertEquals(CONFIRMED_AT, answered.getConfirmedAt());
        assertEquals(CREATED_AT, answered.getCreatedAt());
    }

    @Test
    @DisplayName("응답은 새 객체를 반환하고 원래 약속은 CONFIRMED · 응답 없음 그대로다")
    void answerConfirmation_returnsNewObject_keepsOriginal() {
        Appointment confirmed = confirmed();

        confirmed.answerConfirmation(REQUESTER_ID, ConfirmationAnswer.DONE, null, null, OPENS_AT);

        assertEquals(AppointmentStatus.CONFIRMED, confirmed.getStatus());
        assertNull(confirmed.getResolvedAt());
        assertTrue(confirmed.replyOf(REQUESTER_ID).isEmpty());
    }

    @Test
    @DisplayName("받는 사람이 먼저 DONE으로 답해도 COMPLETED다")
    void answerConfirmation_doneByReceiver_completes() {
        Instant now = OPENS_AT.plus(Duration.ofHours(1));

        Appointment answered = confirmed().answerConfirmation(OWNER_ID, ConfirmationAnswer.DONE, null, null, now);

        assertEquals(AppointmentStatus.COMPLETED, answered.getStatus());
        assertEquals(now, answered.getResolvedAt());
        assertEquals(ConfirmationAnswer.DONE, answered.replyOf(OWNER_ID).orElseThrow().answer());
        assertTrue(answered.replyOf(REQUESTER_ID).isEmpty());
    }

    @Test
    @DisplayName("약속 전 알림을 끈 사람이 DONE으로 답해도 COMPLETED다")
    void answerConfirmation_reminderOff_stillCompletes() {
        Appointment confirmed = appointment(AppointmentStatus.CONFIRMED, scheduled(), null, null, null, List.of());

        Appointment answered = confirmed.answerConfirmation(REQUESTER_ID, ConfirmationAnswer.DONE, null, null, OPENS_AT);

        assertEquals(AppointmentStatus.COMPLETED, answered.getStatus());
    }

    // ---- AC-2 먼저 못 만났어요 ----

    @ParameterizedTest
    @EnumSource(NotMetReason.class)
    @DisplayName("제안자가 NOT_MET과 이유로 답하면 NOT_MET · 결과 시각 now · 이유가 저장된다")
    void answerConfirmation_notMetEachReason_marksNotMet(NotMetReason reason) {
        Instant now = OPENS_AT.plus(Duration.ofMinutes(5));

        Appointment answered = confirmed().answerConfirmation(REQUESTER_ID, ConfirmationAnswer.NOT_MET, reason, null, now);

        assertEquals(AppointmentStatus.NOT_MET, answered.getStatus());
        assertEquals(now, answered.getResolvedAt());
        assertEquals(Optional.of(new ConfirmationReply(REQUESTER_ID, ConfirmationAnswer.NOT_MET, reason, null, now)),
                answered.replyOf(REQUESTER_ID));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "상대가 다른 물건을 가져왔어요"})
    @DisplayName("OTHER의 직접 입력이 null · 빈 문자열 · 공백 · 문장이면 보낸 값 그대로 저장된다")
    void answerConfirmation_otherWithDetail_savesDetailAsIs(String detail) {
        Appointment answered = confirmed().answerConfirmation(REQUESTER_ID, ConfirmationAnswer.NOT_MET,
                NotMetReason.OTHER, detail, OPENS_AT);

        assertEquals(AppointmentStatus.NOT_MET, answered.getStatus());
        assertEquals(detail, answered.replyOf(REQUESTER_ID).orElseThrow().notMetDetail());
    }

    @Test
    @DisplayName("OTHER의 직접 입력 100자도 그대로 저장된다")
    void answerConfirmation_otherWith100Chars_savesDetail() {
        String detail = "가".repeat(100);

        Appointment answered = confirmed().answerConfirmation(REQUESTER_ID, ConfirmationAnswer.NOT_MET,
                NotMetReason.OTHER, detail, OPENS_AT);

        assertEquals(detail, answered.replyOf(REQUESTER_ID).orElseThrow().notMetDetail());
    }

    // ---- AC-3 · AC-4 이유 남기기 ----

    @Test
    @DisplayName("NOT_MET 뒤 상대가 이유를 남기면 NOT_MET · 결과 시각 그대로이고 상대 응답이 추가된다")
    void answerConfirmation_reasonAfterNotMet_appendsReplyOnly() {
        // given
        Appointment notMet = notMetByRequester(OPENS_AT);
        Instant later = OPENS_AT.plus(Duration.ofHours(2));

        // when
        Appointment answered = notMet.answerConfirmation(OWNER_ID, ConfirmationAnswer.NOT_MET,
                NotMetReason.SELF_ABSENT, null, later);

        // then
        assertEquals(AppointmentStatus.NOT_MET, answered.getStatus());
        assertEquals(OPENS_AT, answered.getResolvedAt());
        assertEquals(Optional.of(new ConfirmationReply(OWNER_ID, ConfirmationAnswer.NOT_MET,
                NotMetReason.SELF_ABSENT, null, later)), answered.replyOf(OWNER_ID));
        assertEquals(notMet.replyOf(REQUESTER_ID), answered.replyOf(REQUESTER_ID));
    }

    @Test
    @DisplayName("두 사람이 차례로 NOT_MET으로 답하면 먼저 답한 사람이 결과를 정하고 나중 사람은 이유 남기기가 된다")
    void answerConfirmation_notMetThenNotMet_secondIsReason() {
        Appointment first = confirmed().answerConfirmation(REQUESTER_ID, ConfirmationAnswer.NOT_MET,
                NotMetReason.NO_SHOW, null, OPENS_AT);

        Appointment second = first.answerConfirmation(OWNER_ID, ConfirmationAnswer.NOT_MET,
                NotMetReason.MUTUAL_CANCEL, null, OPENS_AT.plusSeconds(1));

        assertEquals(AppointmentStatus.NOT_MET, second.getStatus());
        assertEquals(OPENS_AT, second.getResolvedAt());
        assertEquals(NotMetReason.NO_SHOW, second.replyOf(REQUESTER_ID).orElseThrow().notMetReason());
        assertEquals(NotMetReason.MUTUAL_CANCEL, second.replyOf(OWNER_ID).orElseThrow().notMetReason());
    }

    @Test
    @DisplayName("열림 시각 + 3일 1초 전에는 이유를 남길 수 있다")
    void answerConfirmation_reasonJustBeforeDeadline_appends() {
        Appointment answered = notMetByRequester(OPENS_AT).answerConfirmation(OWNER_ID, ConfirmationAnswer.NOT_MET,
                NotMetReason.ITEM_MISMATCH, null, REASON_DEADLINE.minusSeconds(1));

        assertEquals(AppointmentStatus.NOT_MET, answered.getStatus());
        assertTrue(answered.replyOf(OWNER_ID).isPresent());
    }

    @ParameterizedTest
    @ValueSource(longs = {0, 1, 86_400})
    @DisplayName("열림 시각 + 3일 정각 이후에 이유를 남기면 EXCHANGE_303")
    void answerConfirmation_reasonAtOrAfterDeadline_throwsAlreadyDecided(long secondsAfterDeadline) {
        Appointment notMet = notMetByRequester(OPENS_AT);

        assertRejected(ExchangeErrorCode.CONFIRMATION_ALREADY_DECIDED, () -> notMet.answerConfirmation(OWNER_ID,
                ConfirmationAnswer.NOT_MET, NotMetReason.SELF_ABSENT, null,
                REASON_DEADLINE.plusSeconds(secondsAfterDeadline)));
    }

    @Test
    @DisplayName("이유 남기기 기한은 결과 시각이 아니라 열림 시각 기준이다")
    void answerConfirmation_deadlineFromOpensAtNotResolvedAt() {
        // 결과가 열림 1일 뒤에 정해져도 기한은 열림 + 3일
        Appointment notMet = notMetByRequester(OPENS_AT.plus(Duration.ofDays(1)));

        assertRejected(ExchangeErrorCode.CONFIRMATION_ALREADY_DECIDED, () -> notMet.answerConfirmation(OWNER_ID,
                ConfirmationAnswer.NOT_MET, NotMetReason.SELF_ABSENT, null, REASON_DEADLINE));
    }

    // ---- AC-5 · AC-6 결과가 정해진 뒤 ----

    @ParameterizedTest
    @EnumSource(ConfirmationAnswer.class)
    @DisplayName("COMPLETED 뒤 상대가 어떤 답을 보내도 EXCHANGE_303")
    void answerConfirmation_afterCompleted_throwsAlreadyDecided(ConfirmationAnswer answer) {
        Appointment completed = completedByRequester();
        NotMetReason reason = answer == ConfirmationAnswer.NOT_MET ? NotMetReason.NO_SHOW : null;

        assertRejected(ExchangeErrorCode.CONFIRMATION_ALREADY_DECIDED, () -> completed.answerConfirmation(OWNER_ID,
                answer, reason, null, OPENS_AT.plusSeconds(10)));
    }

    @Test
    @DisplayName("NOT_MET 뒤 상대가 기한 안에 DONE으로 답하면 EXCHANGE_303")
    void answerConfirmation_doneAfterNotMet_throwsAlreadyDecided() {
        Appointment notMet = notMetByRequester(OPENS_AT);

        assertRejected(ExchangeErrorCode.CONFIRMATION_ALREADY_DECIDED, () -> notMet.answerConfirmation(OWNER_ID,
                ConfirmationAnswer.DONE, null, null, OPENS_AT.plus(Duration.ofHours(1))));
    }

    // ---- AC-7 같은 사람이 다시 답함 ----

    @ParameterizedTest
    @EnumSource(ConfirmationAnswer.class)
    @DisplayName("DONE으로 답한 사람이 다시 답하면 EXCHANGE_302")
    void answerConfirmation_againAfterDone_throwsAlreadyAnswered(ConfirmationAnswer answer) {
        Appointment completed = completedByRequester();
        NotMetReason reason = answer == ConfirmationAnswer.NOT_MET ? NotMetReason.NO_SHOW : null;

        assertRejected(ExchangeErrorCode.CONFIRMATION_ALREADY_ANSWERED, () -> completed.answerConfirmation(
                REQUESTER_ID, answer, reason, null, OPENS_AT.plusSeconds(10)));
    }

    @ParameterizedTest
    @EnumSource(ConfirmationAnswer.class)
    @DisplayName("NOT_MET으로 답한 사람이 다시 답하면 EXCHANGE_302")
    void answerConfirmation_againAfterNotMet_throwsAlreadyAnswered(ConfirmationAnswer answer) {
        Appointment notMet = notMetByRequester(OPENS_AT);
        NotMetReason reason = answer == ConfirmationAnswer.NOT_MET ? NotMetReason.OTHER : null;

        assertRejected(ExchangeErrorCode.CONFIRMATION_ALREADY_ANSWERED, () -> notMet.answerConfirmation(
                REQUESTER_ID, answer, reason, null, OPENS_AT.plusSeconds(10)));
    }

    @Test
    @DisplayName("이유를 남긴 상대가 다시 보내면 EXCHANGE_302")
    void answerConfirmation_againAfterReason_throwsAlreadyAnswered() {
        Appointment bothAnswered = notMetByRequester(OPENS_AT).answerConfirmation(OWNER_ID,
                ConfirmationAnswer.NOT_MET, NotMetReason.SELF_ABSENT, null, OPENS_AT.plusSeconds(10));

        assertRejected(ExchangeErrorCode.CONFIRMATION_ALREADY_ANSWERED, () -> bothAnswered.answerConfirmation(
                OWNER_ID, ConfirmationAnswer.NOT_MET, NotMetReason.OTHER, "다시", OPENS_AT.plusSeconds(20)));
    }

    @Test
    @DisplayName("이미 답한 사람은 이유 남기기 기한이 지났어도 EXCHANGE_302가 먼저다")
    void answerConfirmation_againAfterDeadline_throwsAlreadyAnsweredFirst() {
        Appointment notMet = notMetByRequester(OPENS_AT);

        assertRejected(ExchangeErrorCode.CONFIRMATION_ALREADY_ANSWERED, () -> notMet.answerConfirmation(
                REQUESTER_ID, ConfirmationAnswer.NOT_MET, NotMetReason.NO_SHOW, null, REASON_DEADLINE));
    }

    // ---- AC-8 · AC-9 열림 시각 ----

    @ParameterizedTest
    @ValueSource(longs = {1, 60, 1_800, 3_600})
    @DisplayName("열림 시각 전(약속 시각 전 포함)에 답하면 EXCHANGE_301")
    void answerConfirmation_beforeOpen_throwsNotOpen(long secondsBeforeOpen) {
        Appointment confirmed = confirmed();

        assertRejected(ExchangeErrorCode.CONFIRMATION_NOT_OPEN, () -> confirmed.answerConfirmation(REQUESTER_ID,
                ConfirmationAnswer.DONE, null, null, OPENS_AT.minusSeconds(secondsBeforeOpen)));
    }

    @Test
    @DisplayName("열림 시각 전 NOT_MET도 EXCHANGE_301")
    void answerConfirmation_notMetBeforeOpen_throwsNotOpen() {
        Appointment confirmed = confirmed();

        assertRejected(ExchangeErrorCode.CONFIRMATION_NOT_OPEN, () -> confirmed.answerConfirmation(OWNER_ID,
                ConfirmationAnswer.NOT_MET, NotMetReason.NO_SHOW, null, OPENS_AT.minusSeconds(1)));
    }

    @Test
    @DisplayName("SCHEDULED 약속의 열림 시각은 약속 시각 + 30분이다")
    void confirmationOpensAt_scheduled_isScheduledAtPlus30Minutes() {
        assertEquals(OPENS_AT, confirmed().confirmationOpensAt());
    }

    @Test
    @DisplayName("지금 바로 약속의 열림 시각은 확정 시각 + 30분이다")
    void confirmationOpensAt_now_isConfirmedAtPlus30Minutes() {
        assertEquals(CONFIRMED_AT.plus(Duration.ofMinutes(30)), confirmedNow().confirmationOpensAt());
    }

    @Test
    @DisplayName("확정 시각이 없으면 열림 시각은 null이다")
    void confirmationOpensAt_notConfirmed_isNull() {
        Appointment proposal = appointment(AppointmentStatus.PROPOSED, scheduled(), null, null, null, List.of());

        assertNull(proposal.confirmationOpensAt());
    }

    @Test
    @DisplayName("지금 바로 약속은 확정 + 30분 1초 전에 EXCHANGE_301 · 정각에는 응답된다")
    void answerConfirmation_nowAppointment_opensAtConfirmedPlus30() {
        Appointment confirmed = confirmedNow();
        Instant opensAt = CONFIRMED_AT.plus(Duration.ofMinutes(30));

        assertRejected(ExchangeErrorCode.CONFIRMATION_NOT_OPEN, () -> confirmed.answerConfirmation(REQUESTER_ID,
                ConfirmationAnswer.DONE, null, null, opensAt.minusSeconds(1)));
        Appointment answered = confirmed.answerConfirmation(REQUESTER_ID, ConfirmationAnswer.DONE, null, null, opensAt);
        assertEquals(AppointmentStatus.COMPLETED, answered.getStatus());
    }

    // ---- AC-12 · AC-13 권한 · 상태 ----

    @Test
    @DisplayName("참여자가 아닌 사람이 답하면 EXCHANGE_002")
    void answerConfirmation_thirdParty_throwsNotParticipant() {
        Appointment confirmed = confirmed();

        assertRejected(ExchangeErrorCode.NOT_EXCHANGE_PARTICIPANT, () -> confirmed.answerConfirmation(
                OTHER_MEMBER_ID, ConfirmationAnswer.DONE, null, null, OPENS_AT));
    }

    @Test
    @DisplayName("참여자가 아니면 확정이 아닌 약속이어도 EXCHANGE_002가 먼저다")
    void answerConfirmation_thirdPartyOnRejected_throwsNotParticipantFirst() {
        Appointment rejected = appointment(AppointmentStatus.REJECTED, scheduled(), null, null, null, List.of());

        assertRejected(ExchangeErrorCode.NOT_EXCHANGE_PARTICIPANT, () -> rejected.answerConfirmation(
                OTHER_MEMBER_ID, ConfirmationAnswer.DONE, null, null, OPENS_AT));
    }

    @ParameterizedTest
    @EnumSource(value = AppointmentStatus.class,
            names = {"PROPOSED", "REJECTED", "WITHDRAWN", "EXPIRED", "AUTO_REJECTED", "CANCELED"})
    @DisplayName("확정 · 완료 · 못 만남이 아닌 약속에 답하면 EXCHANGE_109")
    void answerConfirmation_notConfirmedStatus_throwsInvalidStatus(AppointmentStatus status) {
        Instant confirmedAt = status == AppointmentStatus.CANCELED ? CONFIRMED_AT : null;
        Appointment appointment = appointment(status, scheduled(), confirmedAt, null, null, List.of());

        assertRejected(ExchangeErrorCode.INVALID_APPOINTMENT_STATUS, () -> appointment.answerConfirmation(
                REQUESTER_ID, ConfirmationAnswer.DONE, null, null, OPENS_AT));
    }

    @ParameterizedTest
    @ValueSource(longs = {-86_400, 86_400})
    @DisplayName("PROPOSED 약속은 만료 전 · 후 모두 EXCHANGE_109")
    void answerConfirmation_proposedBeforeOrAfterExpiry_throwsInvalidStatus(long expiresOffsetSeconds) {
        Appointment proposal = Appointment.of(APPOINTMENT_ID, EXCHANGE_ID, REQUESTER_ID, OWNER_ID,
                AppointmentStatus.PROPOSED, scheduled(), PLACE, new AppointmentItemIds(List.of(31L)),
                OPENS_AT.plusSeconds(expiresOffsetSeconds), 30, null, null, null, List.of(), CREATED_AT, CREATED_AT);

        assertRejected(ExchangeErrorCode.INVALID_APPOINTMENT_STATUS, () -> proposal.answerConfirmation(
                OWNER_ID, ConfirmationAnswer.NOT_MET, NotMetReason.NO_SHOW, null, OPENS_AT));
    }

    // ---- AC-16 동시(순차) 응답 ----

    @Test
    @DisplayName("DONE 다음 NOT_MET이 오면 COMPLETED로 정해지고 나중 응답은 EXCHANGE_303이다")
    void answerConfirmation_doneThenNotMet_keepsCompleted() {
        Appointment first = confirmed().answerConfirmation(OWNER_ID, ConfirmationAnswer.DONE, null, null, OPENS_AT);

        assertRejected(ExchangeErrorCode.CONFIRMATION_ALREADY_DECIDED, () -> first.answerConfirmation(REQUESTER_ID,
                ConfirmationAnswer.NOT_MET, NotMetReason.NO_SHOW, null, OPENS_AT));
        assertEquals(AppointmentStatus.COMPLETED, first.getStatus());
    }

    // ---- 복원 팩토리 ----

    @Test
    @DisplayName("14개 인자 복원 팩토리는 결과 시각 null · 응답 없음으로 만든다")
    void of_legacyFactory_hasNoReplies() {
        Appointment legacy = Appointment.of(APPOINTMENT_ID, EXCHANGE_ID, REQUESTER_ID, OWNER_ID,
                AppointmentStatus.CONFIRMED, scheduled(), PLACE, new AppointmentItemIds(List.of(31L)), EXPIRES_AT,
                30, 60, CONFIRMED_AT, CREATED_AT, CREATED_AT);

        assertNull(legacy.getResolvedAt());
        assertTrue(legacy.replyOf(REQUESTER_ID).isEmpty());
        assertTrue(legacy.replyOf(OWNER_ID).isEmpty());
    }

    // ---- helpers ----

    /** 예외 코드가 맞는지 확인한다. 도메인 객체는 불변이므로 원래 객체가 바뀌지 않는 것은 별도로 보지 않는다. */
    private static void assertRejected(ExchangeErrorCode expected, Executable action) {
        ExchangeException exception = assertThrows(ExchangeException.class, action);
        assertEquals(expected, exception.getErrorCode());
    }

    /** A가 제안하고 B가 수락한 SCHEDULED 약속 M (제안자 알림 30 · 받는 사람 알림 60) */
    private static Appointment confirmed() {
        return Appointment.of(APPOINTMENT_ID, EXCHANGE_ID, REQUESTER_ID, OWNER_ID, AppointmentStatus.CONFIRMED,
                scheduled(), PLACE, new AppointmentItemIds(List.of(31L)), EXPIRES_AT, 30, 60, CONFIRMED_AT,
                null, List.of(), CREATED_AT, CREATED_AT);
    }

    /** 14:00에 확정된 지금 바로 약속 */
    private static Appointment confirmedNow() {
        return appointment(AppointmentStatus.CONFIRMED, new AppointmentTime(AppointmentTimeType.NOW, null),
                CONFIRMED_AT, null, null, List.of());
    }

    /** A가 DONE으로 답해 COMPLETED된 약속 (복원) */
    private static Appointment completedByRequester() {
        return appointment(AppointmentStatus.COMPLETED, scheduled(), CONFIRMED_AT, 30, OPENS_AT,
                List.of(new ConfirmationReply(REQUESTER_ID, ConfirmationAnswer.DONE, null, null, OPENS_AT)));
    }

    /** A가 NO_SHOW로 답해 NOT_MET된 약속 (복원) */
    private static Appointment notMetByRequester(Instant resolvedAt) {
        return appointment(AppointmentStatus.NOT_MET, scheduled(), CONFIRMED_AT, 30, resolvedAt,
                List.of(new ConfirmationReply(REQUESTER_ID, ConfirmationAnswer.NOT_MET, NotMetReason.NO_SHOW, null,
                        resolvedAt)));
    }

    private static Appointment appointment(AppointmentStatus status, AppointmentTime time, Instant confirmedAt,
                                           Integer proposerReminder, Instant resolvedAt,
                                           List<ConfirmationReply> replies) {
        return Appointment.of(APPOINTMENT_ID, EXCHANGE_ID, REQUESTER_ID, OWNER_ID, status, time, PLACE,
                new AppointmentItemIds(List.of(31L)), EXPIRES_AT, proposerReminder, null, confirmedAt,
                resolvedAt, replies, CREATED_AT, CREATED_AT);
    }

    private static AppointmentTime scheduled() {
        return new AppointmentTime(AppointmentTimeType.SCHEDULED, SCHEDULED_AT);
    }
}
