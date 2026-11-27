package com.ject.exchange.application.service.query;

import com.ject.common.core.time.ServiceTime;
import com.ject.exchange.application.dto.dto.ExchangeResult;
import com.ject.exchange.application.dto.dto.Participant;
import com.ject.exchange.application.dto.dto.PostInfo;
import com.ject.exchange.application.dto.dto.UserInfo;
import com.ject.exchange.application.dto.query.GetExchangeQuery;
import com.ject.exchange.domain.domain.exchange.Exchange;
import com.ject.exchange.domain.domain.exchange.ExchangeStatus;
import com.ject.exchange.domain.domain.exchange.OfferType;
import com.ject.exchange.domain.domain.exchange.OfferedItem;
import com.ject.exchange.domain.domain.exchange.vo.ItemDescription;
import com.ject.exchange.domain.domain.exchange.vo.ItemName;
import com.ject.exchange.domain.exception.ExchangeErrorCode;
import com.ject.exchange.domain.exception.ExchangeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.time.Clock;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * E-2 교환 상세의 참여자 판정과 요청자 · 게시자 회원 정보 채우기를 확인한다.
 */
class GetExchangeHandlerParticipantTest {

    private static final Instant NOW = Instant.parse("2026-11-21T05:00:00Z");
    private static final Long EXCHANGE_ID = 1L;
    private static final Long POST_ID = 10L;
    private static final Long ITEM_ID = 31L;
    private static final Long MEMBER_A = 101L;
    private static final Long MEMBER_B = 102L;
    private static final Long MEMBER_C = 103L;
    private static final String WITHDRAWN_NICKNAME = "탈퇴한 사용자";

    private FakeExchangeRepository exchangeRepository;
    private FakePostPort postPort;
    private FakeUserPort userPort;
    private GetExchangeHandler handler;

    @BeforeEach
    void setUp() {
        exchangeRepository = new FakeExchangeRepository();
        postPort = new FakePostPort();
        userPort = new FakeUserPort();
        handler = new GetExchangeHandler(exchangeRepository, new FakeAppointmentRepository(exchangeRepository, NOW),
                postPort, userPort, new ServiceTime(Clock.fixed(NOW, ZoneId.of("Asia/Seoul"))), new FakeChatRoomPort());

        postPort.register(new PostInfo(POST_ID, "치이카와 교환해요", null, MEMBER_B, PostInfo.Status.AVAILABLE));
        exchangeRepository.preload(exchangeOf(MEMBER_A, MEMBER_B));
    }

    @Test
    @DisplayName("요청자 · 게시자의 닉네임과 교환 완료 횟수를 회원 정보로 채운다")
    void handle_fillsParticipantSummaries() {
        // given
        userPort.register(new UserInfo(MEMBER_A, "바꾸", 3));
        userPort.register(new UserInfo(MEMBER_B, "하치", 0));

        // when
        ExchangeResult result = handler.handle(new GetExchangeQuery(EXCHANGE_ID, MEMBER_A));

        // then
        assertParticipant(result.requester(), MEMBER_A, "바꾸", 3);
        assertParticipant(result.owner(), MEMBER_B, "하치", 0);
    }

    @Test
    @DisplayName("회원 정보는 요청자와 게시자 id로 조회한다")
    void handle_requestsBothParticipantIds() {
        userPort.register(new UserInfo(MEMBER_A, "바꾸", 3));
        userPort.register(new UserInfo(MEMBER_B, "하치", 0));

        handler.handle(new GetExchangeQuery(EXCHANGE_ID, MEMBER_B));

        List<Long> requested = userPort.requestedIds().stream().flatMap(List::stream).distinct().toList();
        assertEquals(2, requested.size());
        assertEquals(true, requested.containsAll(List.of(MEMBER_A, MEMBER_B)));
    }

    @Test
    @DisplayName("게시자가 탈퇴했으면 닉네임은 탈퇴한 사용자이고 id와 완료 횟수는 그대로다")
    void handle_withdrawnOwner_keepsIdAndCount() {
        userPort.register(new UserInfo(MEMBER_A, "바꾸", 3));
        userPort.register(new UserInfo(MEMBER_B, WITHDRAWN_NICKNAME, 5));

        ExchangeResult result = handler.handle(new GetExchangeQuery(EXCHANGE_ID, MEMBER_A));

        assertParticipant(result.owner(), MEMBER_B, WITHDRAWN_NICKNAME, 5);
        assertParticipant(result.requester(), MEMBER_A, "바꾸", 3);
    }

    @Test
    @DisplayName("게시자 회원 정보가 없으면 게시자는 교환의 id · 닉네임 null · 완료 0이고 요청자는 정상이다")
    void handle_missingOwnerMember_fillsDefaults() {
        userPort.register(new UserInfo(MEMBER_A, "바꾸", 3));

        ExchangeResult result = handler.handle(new GetExchangeQuery(EXCHANGE_ID, MEMBER_A));

        assertEquals(MEMBER_B, result.owner().userId());
        assertNull(result.owner().nickname());
        assertEquals(Integer.valueOf(0), result.owner().completedCount());
        assertParticipant(result.requester(), MEMBER_A, "바꾸", 3);
    }

    @Test
    @DisplayName("두 참여자 모두 회원 정보가 없어도 예외 없이 닉네임 null · 완료 0으로 채운다")
    void handle_bothMembersMissing_fillsDefaults() {
        ExchangeResult result = handler.handle(new GetExchangeQuery(EXCHANGE_ID, MEMBER_B));

        assertEquals(MEMBER_A, result.requester().userId());
        assertNull(result.requester().nickname());
        assertEquals(Integer.valueOf(0), result.requester().completedCount());
        assertEquals(MEMBER_B, result.owner().userId());
        assertNull(result.owner().nickname());
        assertEquals(Integer.valueOf(0), result.owner().completedCount());
    }

    @Test
    @DisplayName("요청자가 조회하면 교환 상세를 돌려준다")
    void handle_byRequester_returnsResult() {
        ExchangeResult result = handler.handle(new GetExchangeQuery(EXCHANGE_ID, MEMBER_A));

        assertEquals(EXCHANGE_ID, result.id());
    }

    @Test
    @DisplayName("게시자가 조회하면 교환 상세를 돌려준다")
    void handle_byOwner_returnsResult() {
        ExchangeResult result = handler.handle(new GetExchangeQuery(EXCHANGE_ID, MEMBER_B));

        assertEquals(EXCHANGE_ID, result.id());
    }

    @Test
    @DisplayName("참여자가 아닌 회원이 조회하면 NOT_EXCHANGE_PARTICIPANT")
    void handle_byNonParticipant_throwsNotParticipant() {
        ExchangeException exception = assertThrows(ExchangeException.class,
                () -> handler.handle(new GetExchangeQuery(EXCHANGE_ID, MEMBER_C)));

        assertEquals(ExchangeErrorCode.NOT_EXCHANGE_PARTICIPANT, exception.getErrorCode());
    }

    private static void assertParticipant(Participant participant, Long userId, String nickname, int completedCount) {
        assertEquals(userId, participant.userId());
        assertEquals(nickname, participant.nickname());
        assertEquals(Integer.valueOf(completedCount), participant.completedCount());
    }

    private static Exchange exchangeOf(Long requesterId, Long ownerId) {
        return Exchange.of(EXCHANGE_ID, POST_ID, requesterId, ownerId, OfferType.NEW,
                List.of(OfferedItem.of(ITEM_ID, null, new ItemName("치이카와 아크릴 스탠드"), false,
                        new ItemDescription("가챠에서 두 개 나와서 하나 교환해요"), 0)),
                ExchangeStatus.ACTIVE, null, NOW, NOW);
    }
}
