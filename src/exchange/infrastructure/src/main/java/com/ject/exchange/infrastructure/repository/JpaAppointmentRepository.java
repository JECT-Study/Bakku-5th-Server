package com.ject.exchange.infrastructure.repository;

import com.ject.exchange.domain.domain.appointment.Appointment;
import com.ject.exchange.domain.domain.appointment.AppointmentStatus;
import com.ject.exchange.domain.repository.AppointmentRepository;
import com.ject.exchange.infrastructure.entity.AppointmentEntity;
import com.ject.exchange.infrastructure.entity.AppointmentItemEntity;
import com.ject.exchange.infrastructure.jpa.AppointmentEntityRepository;
import com.ject.exchange.infrastructure.jpa.AppointmentItemEntityRepository;
import com.ject.exchange.infrastructure.mapper.AppointmentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Repository
@RequiredArgsConstructor
public class JpaAppointmentRepository implements AppointmentRepository {

    private final AppointmentEntityRepository appointmentEntityRepository;
    private final AppointmentItemEntityRepository appointmentItemEntityRepository;

    @Override
    @Transactional
    public Appointment save(Appointment appointment) {
        AppointmentEntity saved = appointmentEntityRepository.save(AppointmentMapper.toEntity(appointment));
        // 고른 물건은 제안 시점에 정해지고 이후 바뀌지 않으므로 새 약속일 때만 저장한다
        List<AppointmentItemEntity> items = appointment.getId() == null
                ? appointmentItemEntityRepository.saveAll(AppointmentMapper.toItemEntities(saved, appointment))
                : appointmentItemEntityRepository.findByAppointmentIdOrderBySortOrder(saved.getId());
        return AppointmentMapper.toDomain(saved, items);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Appointment> findById(Long id) {
        return appointmentEntityRepository.findById(id)
                .map(entity -> AppointmentMapper.toDomain(
                        entity, appointmentItemEntityRepository.findByAppointmentIdOrderBySortOrder(id)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Appointment> findAllByExchangeId(Long exchangeId) {
        List<AppointmentEntity> appointments = appointmentEntityRepository.findByExchangeIdOrderById(exchangeId);
        if (appointments.isEmpty()) {
            return List.of();
        }
        // 약속마다 아이템을 따로 조회하지 않도록 한 번에 가져와 약속별로 나눈다 (제안 시 교환 잠금을 잡은 채 호출된다)
        Map<Long, List<AppointmentItemEntity>> itemsByAppointmentId = appointmentItemEntityRepository
                .findByAppointmentIdInOrderBySortOrder(appointments.stream().map(AppointmentEntity::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(item -> item.getAppointment().getId()));
        return appointments.stream()
                .map(entity -> AppointmentMapper.toDomain(
                        entity, itemsByAppointmentId.getOrDefault(entity.getId(), List.of())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsPendingProposalOnPost(Long postId, Long excludedExchangeId, Instant now) {
        return appointmentEntityRepository.existsByPostAndStatusAfter(
                postId, excludedExchangeId, AppointmentStatus.PROPOSED, now);
    }

    @Override
    public Optional<Appointment> findByIdForUpdate(Long id) {
        return appointmentEntityRepository.findWithLockById(id)
                .map(entity -> AppointmentMapper.toDomain(
                        entity, appointmentItemEntityRepository.findByAppointmentIdOrderBySortOrder(id)));
    }

    @Override
    public List<Long> findPostIdsOf(Long appointmentId) {
        // 잠금 전에 쓰므로 엔티티를 읽지 않고 ID만 조회한다. 엔티티가 영속성 컨텍스트에 남으면 잠금 뒤 다시 읽어도 이전 상태가 보인다
        return appointmentEntityRepository.findExchangePostIdByAppointmentId(appointmentId)
                .map(postId -> Stream.concat(Stream.of(postId),
                                appointmentEntityRepository.findChosenSourcePostIdsByAppointmentId(appointmentId).stream())
                        .distinct()
                        .toList())
                .orElse(List.of());
    }

    @Override
    public Set<Long> findPostIdsBoundBy(Collection<Long> postIds, AppointmentStatus status, Long excludedExchangeId) {
        if (postIds.isEmpty()) {
            return Set.of();
        }
        return Stream.concat(
                        appointmentEntityRepository.findBoundExchangePostIds(postIds, status, excludedExchangeId).stream(),
                        appointmentEntityRepository.findBoundChosenSourcePostIds(postIds, status, excludedExchangeId).stream())
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public List<Appointment> findPendingProposalsInvolving(Collection<Long> postIds, Long excludedExchangeId, Instant now) {
        if (postIds.isEmpty()) {
            return List.of();
        }
        List<Long> appointmentIds = Stream.concat(
                        appointmentEntityRepository.findIdsByExchangePostAndStatusAfter(
                                postIds, excludedExchangeId, AppointmentStatus.PROPOSED, now).stream(),
                        appointmentEntityRepository.findIdsByChosenSourcePostAndStatusAfter(
                                postIds, excludedExchangeId, AppointmentStatus.PROPOSED, now).stream())
                .distinct()
                .sorted()
                .toList();
        if (appointmentIds.isEmpty()) {
            return List.of();
        }
        Map<Long, List<AppointmentItemEntity>> itemsByAppointmentId = appointmentItemEntityRepository
                .findByAppointmentIdInOrderBySortOrder(appointmentIds)
                .stream()
                .collect(Collectors.groupingBy(item -> item.getAppointment().getId()));
        return appointmentEntityRepository.findAllById(appointmentIds).stream()
                .map(entity -> AppointmentMapper.toDomain(
                        entity, itemsByAppointmentId.getOrDefault(entity.getId(), List.of())))
                .toList();
    }
}
