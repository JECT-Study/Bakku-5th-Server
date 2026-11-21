package com.ject.exchange.api.controller;

import com.ject.common.core.cqrs.query.Query;
import com.ject.common.core.cqrs.query.QueryBus;
import com.ject.exchange.application.dto.dto.AppointmentResult;
import com.ject.exchange.application.dto.dto.ExchangeResult;
import com.ject.exchange.application.dto.dto.PostInfo;
import com.ject.exchange.application.dto.dto.UserInfo;
import com.ject.exchange.application.dto.query.GetAppointmentQuery;
import com.ject.exchange.application.dto.query.GetExchangeQuery;
import com.ject.exchange.domain.domain.exchange.Exchange;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 테스트용 가짜 쿼리 버스. 교환 상세 조회는 가짜 저장소의 현재 값으로 응답한다.
 * 약속 조회는 테스트가 정한 결과를 돌려준다.
 */
class FakeQueryBus implements QueryBus {

    private final FakeExchangeRepository exchangeRepository;
    private final List<Query<?>> sentQueries = new ArrayList<>();
    private AppointmentResult appointmentResult;
    private RuntimeException appointmentFailure;

    FakeQueryBus(FakeExchangeRepository exchangeRepository) {
        this.exchangeRepository = exchangeRepository;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> R send(Query<R> query) {
        sentQueries.add(query);
        if (query instanceof GetExchangeQuery getExchangeQuery) {
            Exchange exchange = exchangeRepository.findById(getExchangeQuery.exchangeId()).orElseThrow();
            Map<Long, PostInfo> posts = Map.of(exchange.getPostId(), new PostInfo(exchange.getPostId(),
                    "치이카와 교환해요", null, exchange.getOwnerId(), PostInfo.Status.values()[0]));
            Map<Long, UserInfo> users = Map.of(
                    exchange.getRequestUserId(), new UserInfo(exchange.getRequestUserId(), "하치와레러버"),
                    exchange.getOwnerId(), new UserInfo(exchange.getOwnerId(), "쿠로미덕후"));
            return (R) ExchangeResult.of(exchange, posts, users);
        }
        if (query instanceof GetAppointmentQuery && appointmentFailure != null) {
            throw appointmentFailure;
        }
        if (query instanceof GetAppointmentQuery && appointmentResult != null) {
            return (R) appointmentResult;
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

    List<Query<?>> sentQueries() {
        return sentQueries;
    }
}
