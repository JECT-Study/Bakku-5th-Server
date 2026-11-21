package com.ject.exchange.domain.repository;

import com.ject.exchange.domain.domain.appointment.Appointment;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository {

    Appointment save(Appointment appointment);

    Optional<Appointment> findById(Long id);

    List<Appointment> findAllByExchangeId(Long exchangeId);

    /**
     * 같은 글의 다른 교환에 만료 전 제안(PROPOSED)이 있는지
     * @param postId 요청받은 교환글 ID
     * @param excludedExchangeId 제외할 교환 ID (지금 보고 있는 교환)
     * @param now 만료 판단 기준 시각
     */
    boolean existsPendingProposalOnPost(Long postId, Long excludedExchangeId, Instant now);
}
