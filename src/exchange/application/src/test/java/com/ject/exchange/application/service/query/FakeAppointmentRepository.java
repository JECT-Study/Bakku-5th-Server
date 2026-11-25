package com.ject.exchange.application.service.query;

import com.ject.exchange.domain.domain.appointment.Appointment;
import com.ject.exchange.domain.domain.appointment.AppointmentStatus;
import com.ject.exchange.domain.domain.exchange.Exchange;
import com.ject.exchange.domain.domain.exchange.OfferedItem;
import com.ject.exchange.domain.repository.AppointmentRepository;
import com.ject.exchange.domain.repository.ExchangeRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * 테스트용 가짜 약속 저장소. 새 약속에 id와 생성 시각을 채우고, 같은 글의 다른 교환 판단은 교환 저장소로 글 id를 찾아 흉내 낸다.
 */
class FakeAppointmentRepository implements AppointmentRepository {

    private final ExchangeRepository exchangeRepository;
    private final Instant createdAt;
    private final Map<Long, Appointment> store = new LinkedHashMap<>();
    private long nextId = 1000L;
    private int saveCount = 0;
    private int lockedLookupCount = 0;

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
    public boolean existsPendingProposalOnPost(Long postId, Long excludedExchangeId) {
        return store.values().stream()
                .filter(appointment -> !appointment.getExchangeId().equals(excludedExchangeId))
                .filter(appointment -> exchangeRepository.findById(appointment.getExchangeId())
                        .map(exchange -> exchange.getPostId().equals(postId))
                        .orElse(false))
                .anyMatch(appointment -> appointment.getStatus() == AppointmentStatus.PROPOSED);
    }

    /** 잠금은 DB에서만 의미가 있으므로 Fake는 findById와 같다. 호출 횟수만 센다. */
    @Override
    public Optional<Appointment> findByIdForUpdate(Long id) {
        lockedLookupCount++;
        return findById(id);
    }

    @Override
    public List<Long> findPostIdsOf(Long appointmentId) {
        Appointment appointment = store.get(appointmentId);
        if (appointment == null) {
            return List.of();
        }
        return exchangeRepository.findById(appointment.getExchangeId())
                .map(exchange -> postIdsInvolvedIn(exchange, appointment))
                .orElse(List.of());
    }

    @Override
    public Set<Long> findPostIdsBoundBy(Collection<Long> postIds, AppointmentStatus status, Long excludedExchangeId) {
        Set<Long> bound = new LinkedHashSet<>();
        store.values().stream()
                .filter(appointment -> appointment.getStatus() == status)
                .filter(appointment -> !appointment.getExchangeId().equals(excludedExchangeId))
                .forEach(appointment -> exchangeRepository.findById(appointment.getExchangeId())
                        .ifPresent(exchange -> postIdsInvolvedIn(exchange, appointment).stream()
                                .filter(postIds::contains)
                                .forEach(bound::add)));
        return bound;
    }

    @Override
    public List<Appointment> findPendingProposalsInvolving(Collection<Long> postIds, Long excludedExchangeId) {
        return store.values().stream()
                .filter(appointment -> appointment.getStatus() == AppointmentStatus.PROPOSED)
                .filter(appointment -> !appointment.getExchangeId().equals(excludedExchangeId))
                .filter(appointment -> exchangeRepository.findById(appointment.getExchangeId())
                        .map(exchange -> postIdsInvolvedIn(exchange, appointment).stream()
                                .anyMatch(postIds::contains))
                        .orElse(false))
                .toList();
    }

    @Override
    public List<Long> findIdsByMemberIdAndStatuses(Long memberId, Collection<AppointmentStatus> statuses) {
        return store.values().stream()
                .filter(appointment -> appointment.getProposerUserId().equals(memberId)
                        || appointment.getReceiverUserId().equals(memberId))
                .filter(appointment -> statuses.contains(appointment.getStatus()))
                .map(Appointment::getId)
                .sorted()
                .toList();
    }

    /** 교환의 글 + 약속이 고른 아이템의 원본 글. 교환 상태는 보지 않는다. */
    private static List<Long> postIdsInvolvedIn(Exchange exchange, Appointment appointment) {
        List<Long> postIds = new ArrayList<>();
        postIds.add(exchange.getPostId());
        appointment.getItemIds().values().stream()
                .map(itemId -> exchange.getOfferedItems().stream()
                        .filter(item -> item.getId().equals(itemId))
                        .map(OfferedItem::getSourcePostId)
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse(null))
                .filter(Objects::nonNull)
                .forEach(postIds::add);
        return postIds;
    }

    int lockedLookupCount() {
        return lockedLookupCount;
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
