package com.ject.exchange.api.controller;

import com.ject.common.core.cqrs.query.Query;
import com.ject.common.core.cqrs.query.QueryBus;
import com.ject.exchange.application.dto.dto.AppointmentResult;
import com.ject.exchange.application.dto.dto.ExchangeResult;
import com.ject.exchange.application.dto.dto.PostInfo;
import com.ject.exchange.application.dto.dto.UserInfo;
import com.ject.exchange.application.dto.query.GetAppointmentQuery;
import com.ject.exchange.application.dto.query.GetCurrentAppointmentQuery;
import com.ject.exchange.application.dto.query.GetExchangeQuery;
import com.ject.exchange.application.service.query.GetExchangeHandler;
import com.ject.exchange.domain.domain.exchange.Exchange;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 테스트용 가짜 쿼리 버스. 교환 상세 조회는 실제 Handler가 있으면 그 Handler로, 없으면 가짜 저장소의 현재 값으로 응답한다.
 * 약속 조회는 테스트가 정한 결과를 돌려준다.
 */
class FakeQueryBus implements QueryBus {

    private final FakeExchangeRepository exchangeRepository;
    private final GetExchangeHandler getExchangeHandler;
    private final List<Query<?>> sentQueries = new ArrayList<>();
    private AppointmentResult appointmentResult;
    private RuntimeException appointmentFailure;
    private Optional<AppointmentResult> currentAppointment;
    private RuntimeException currentAppointmentFailure;

    FakeQueryBus(FakeExchangeRepository exchangeRepository) {
        this.exchangeRepository = exchangeRepository;
        this.getExchangeHandler = null;
    }

    FakeQueryBus(GetExchangeHandler getExchangeHandler) {
        this.exchangeRepository = null;
        this.getExchangeHandler = getExchangeHandler;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> R send(Query<R> query) {
        sentQueries.add(query);
        if (query instanceof GetExchangeQuery getExchangeQuery && getExchangeHandler != null) {
            return (R) getExchangeHandler.handle(getExchangeQuery);
        }
        if (query instanceof GetExchangeQuery getExchangeQuery) {
            Exchange exchange = exchangeRepository.findById(getExchangeQuery.exchangeId()).orElseThrow();
            Map<Long, PostInfo> posts = Map.of(exchange.getPostId(), new PostInfo(exchange.getPostId(),
                    "치이카와 교환해요", null, exchange.getOwnerId(), PostInfo.Status.values()[0]));
            Map<Long, UserInfo> users = Map.of(
                    exchange.getRequestUserId(), new UserInfo(exchange.getRequestUserId(), "하치와레러버", 0),
                    exchange.getOwnerId(), new UserInfo(exchange.getOwnerId(), "쿠로미덕후", 0));
            return (R) ExchangeResult.of(exchange, posts, users, Set.of());
        }
        if (query instanceof GetAppointmentQuery && appointmentFailure != null) {
            throw appointmentFailure;
        }
        if (query instanceof GetAppointmentQuery && appointmentResult != null) {
            return (R) appointmentResult;
        }
        if (query instanceof GetCurrentAppointmentQuery && currentAppointmentFailure != null) {
            throw currentAppointmentFailure;
        }
        if (query instanceof GetCurrentAppointmentQuery && currentAppointment != null) {
            return (R) currentAppointment;
        }
        throw new IllegalStateException("이 테스트가 다루지 않는 쿼리: " + query);
    }

    void willReturnAppointment(AppointmentResult result) {
        this.appointmentResult = result;
    }

    /** 약속 조회가 Handler 예외(없는 약속 · 참여자 아님)로 끝나는 경우를 흉내 낸다. */
    void willFailAppointment(RuntimeException failure) {
        this.appointmentFailure = failure;
    }

    /** 현재 약속 조회 결과. 유효한 약속이 없으면 빈 Optional을 넘긴다. */
    void willReturnCurrentAppointment(Optional<AppointmentResult> result) {
        this.currentAppointment = result;
    }

    /** 현재 약속 조회가 Handler 예외(없는 교환 · 참여자 아님)로 끝나는 경우를 흉내 낸다. */
    void willFailCurrentAppointment(RuntimeException failure) {
        this.currentAppointmentFailure = failure;
    }

    List<Query<?>> sentQueries() {
        return sentQueries;
    }
}
