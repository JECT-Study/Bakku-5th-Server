package com.ject.exchange.application.service.command;

import com.ject.exchange.domain.domain.appointment.Appointment;
import com.ject.exchange.domain.repository.AppointmentRepository;
import com.ject.exchange.domain.repository.ExchangeRepository;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 테스트용 가짜 약속 저장소. 새 약속에 id와 생성 시각을 채우고, 같은 글의 다른 교환 판단은 교환 저장소로 글 id를 찾아 흉내 낸다.
 */
class FakeAppointmentRepository implements AppointmentRepository {

    private final ExchangeRepository exchangeRepository;
    private final Instant createdAt;
    private final Map<Long, Appointment> store = new LinkedHashMap<>();
    private long nextId = 1000L;
    private int saveCount = 0;

    FakeAppointmentRepository(ExchangeRepository exchangeRepository, Instant createdAt) {
        this.exchangeRepository = exchangeRepository;
        this.createdAt = createdAt;
    }

    @Override
    public Appointment save(Appointment appointment) {
        saveCount++;
        Appointment saved = appointment.getId() != null ? appointment : withIdentity(appointment, nextId++);
        store.put(saved.getId(), saved);
        return saved;
    }

    @Override
    public Optional<Appointment> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Appointment> findAllByExchangeId(Long exchangeId) {
        return store.values().stream()
                .filter(appointment -> appointment.getExchangeId().equals(exchangeId))
                .toList();
    }

    @Override
    public boolean existsPendingProposalOnPost(Long postId, Long excludedExchangeId, Instant now) {
        return store.values().stream()
                .filter(appointment -> !appointment.getExchangeId().equals(excludedExchangeId))
                .filter(appointment -> exchangeRepository.findById(appointment.getExchangeId())
                        .map(exchange -> exchange.getPostId().equals(postId))
                        .orElse(false))
                .anyMatch(appointment -> appointment.isPendingProposalAt(now));
    }

    /** 테스트 준비용 저장. 저장 횟수에 포함하지 않는다. */
    void preload(Appointment appointment) {
        store.put(appointment.getId(), appointment);
    }

    int saveCount() {
        return saveCount;
    }

    List<Appointment> all() {
        return List.copyOf(store.values());
    }

    private Appointment withIdentity(Appointment appointment, Long id) {
        return Appointment.of(id, appointment.getExchangeId(), appointment.getProposerUserId(),
                appointment.getReceiverUserId(), appointment.getStatus(), appointment.getTime(),
                appointment.getPlace(), appointment.getItemIds(), appointment.getExpiresAt(),
                appointment.getProposerReminderOffsetMinutes(), appointment.getReceiverReminderOffsetMinutes(),
                appointment.getConfirmedAt(), createdAt, createdAt);
    }
}
