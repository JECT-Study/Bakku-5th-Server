package com.ject.exchange.application.service.contract;

import com.ject.exchange.application.contract.ExchangeParticipantLookup;
import com.ject.exchange.domain.domain.exchange.Exchange;
import com.ject.exchange.domain.domain.exchange.ExchangeStatus;
import com.ject.exchange.domain.domain.exchange.OfferType;
import com.ject.exchange.domain.domain.exchange.OfferedItem;
import com.ject.exchange.domain.domain.exchange.vo.ItemDescription;
import com.ject.exchange.domain.domain.exchange.vo.ItemName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 다른 BC(image)가 교환 사진 조회 권한을 판단할 때 쓰는 참여자 확인 계약을 확인한다 (#108).
 */
class ExchangeParticipantLookupServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-23T05:00:00Z");
    private static final Long EXCHANGE_ID = 1L;
    private static final Long MISSING_EXCHANGE_ID = 999L;
    private static final Long POST_P = 10L;
    private static final Long MEMBER_A = 101L;
    private static final Long MEMBER_B = 102L;
    private static final Long MEMBER_C = 103L;

    private FakeExchangeRepository exchangeRepository;
    private ExchangeParticipantLookup lookup;

    @BeforeEach
    void setUp() {
        exchangeRepository = new FakeExchangeRepository();
        lookup = new ExchangeParticipantLookupService(exchangeRepository);
        exchangeRepository.preload(exchange(ExchangeStatus.ACTIVE));
    }

    @Test
    @DisplayName("요청자 A면 true다")
    void isParticipant_requester_returnsTrue() {
        assertTrue(lookup.isParticipant(EXCHANGE_ID, MEMBER_A));
    }

    @Test
    @DisplayName("게시자 B면 true다")
    void isParticipant_owner_returnsTrue() {
        assertTrue(lookup.isParticipant(EXCHANGE_ID, MEMBER_B));
    }

    @Test
    @DisplayName("참여자가 아닌 회원 C면 false다")
    void isParticipant_thirdParty_returnsFalse() {
        assertFalse(lookup.isParticipant(EXCHANGE_ID, MEMBER_C));
    }

    @Test
    @DisplayName("없는 교환이면 예외 없이 false다")
    void isParticipant_missingExchange_returnsFalse() {
        assertFalse(lookup.isParticipant(MISSING_EXCHANGE_ID, MEMBER_A));
    }

    @Test
    @DisplayName("회원 id가 null이면 false다")
    void isParticipant_nullMember_returnsFalse() {
        assertFalse(lookup.isParticipant(EXCHANGE_ID, null));
    }

    @ParameterizedTest
    @EnumSource(value = ExchangeStatus.class, names = {"COMPLETED", "CLOSED", "LEFT"})
    @DisplayName("종료된 교환이어도 요청자 · 게시자면 true다")
    void isParticipant_closedExchange_stillTrueForParticipants(ExchangeStatus status) {
        exchangeRepository.preload(exchange(status));

        assertTrue(lookup.isParticipant(EXCHANGE_ID, MEMBER_A));
        assertTrue(lookup.isParticipant(EXCHANGE_ID, MEMBER_B));
        assertFalse(lookup.isParticipant(EXCHANGE_ID, MEMBER_C));
    }

    private static Exchange exchange(ExchangeStatus status) {
        Instant completedAt = status == ExchangeStatus.COMPLETED ? NOW : null;
        return Exchange.of(EXCHANGE_ID, POST_P, MEMBER_A, MEMBER_B, OfferType.NEW,
                List.of(OfferedItem.of(31L, null, new ItemName("치이카와 아크릴 스탠드"), false,
                        new ItemDescription(null), 0, List.of("exchange/1/0b1c2d3e-0001.jpg"))),
                status, completedAt, NOW, NOW);
    }
}
