package com.ject.exchange.infrastructure.repository;

import com.ject.exchange.domain.domain.appointment.Appointment;
import com.ject.exchange.domain.domain.appointment.AppointmentStatus;
import com.ject.exchange.domain.repository.AppointmentRepository;
import com.ject.exchange.infrastructure.entity.AppointmentConfirmationEntity;
import com.ject.exchange.infrastructure.entity.AppointmentEntity;
import com.ject.exchange.infrastructure.entity.AppointmentItemEntity;
import com.ject.exchange.infrastructure.jpa.AppointmentConfirmationEntityRepository;
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
    private final AppointmentConfirmationEntityRepository appointmentConfirmationEntityRepository;

    @Override
    @Transactional
    public Appointment save(Appointment appointment) {
        AppointmentEntity saved = appointmentEntityRepository.save(AppointmentMapper.toEntity(appointment));
        // 고른 물건은 제안 시점에 정해지고 이후 바뀌지 않으므로 새 약속일 때만 저장한다
        List<AppointmentItemEntity> items = appointment.getId() == null
                ? appointmentItemEntityRepository.saveAll(AppointmentMapper.toItemEntities(saved, appointment))
                : appointmentItemEntityRepository.findByAppointmentIdOrderBySortOrder(saved.getId());
        List<AppointmentConfirmationEntity> stored = appointment.getId() == null
                ? List.of()
                : appointmentConfirmationEntityRepository.findByAppointmentIdOrderByAnsweredAtAscIdAsc(saved.getId());
        // 같은 사람의 응답이 동시에 들어와도 유일 제약으로 하나만 남는다. 즉시 반영해 그 위반이 이 트랜잭션에서 드러나게 한다
        appointmentConfirmationEntityRepository.saveAllAndFlush(
                AppointmentMapper.toNewConfirmationEntities(saved, appointment, stored));
        return AppointmentMapper.toDomain(saved, items,
                appointmentConfirmationEntityRepository.findByAppointmentIdOrderByAnsweredAtAscIdAsc(saved.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Appointment> findById(Long id) {
        return appointmentEntityRepository.findById(id).map(this::toDomain);
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
        Map<Long, List<AppointmentConfirmationEntity>> confirmationsByAppointmentId =
                confirmationsByAppointmentId(appointments.stream().map(AppointmentEntity::getId).toList());
        return appointments.stream()
                .map(entity -> AppointmentMapper.toDomain(
                        entity, itemsByAppointmentId.getOrDefault(entity.getId(), List.of()),
                        confirmationsByAppointmentId.getOrDefault(entity.getId(), List.of())))
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
        return appointmentEntityRepository.findWithLockById(id).map(this::toDomain);
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
        Map<Long, List<AppointmentConfirmationEntity>> confirmationsByAppointmentId =
                confirmationsByAppointmentId(appointmentIds);
        return appointmentEntityRepository.findAllById(appointmentIds).stream()
                .map(entity -> AppointmentMapper.toDomain(
                        entity, itemsByAppointmentId.getOrDefault(entity.getId(), List.of()),
                        confirmationsByAppointmentId.getOrDefault(entity.getId(), List.of())))
                .toList();
    }

    private Appointment toDomain(AppointmentEntity entity) {
        return AppointmentMapper.toDomain(entity,
                appointmentItemEntityRepository.findByAppointmentIdOrderBySortOrder(entity.getId()),
                appointmentConfirmationEntityRepository.findByAppointmentIdOrderByAnsweredAtAscIdAsc(entity.getId()));
    }

    private Map<Long, List<AppointmentConfirmationEntity>> confirmationsByAppointmentId(List<Long> appointmentIds) {
        return appointmentConfirmationEntityRepository.findByAppointmentIdInOrderByAnsweredAtAscIdAsc(appointmentIds)
                .stream()
                .collect(Collectors.groupingBy(confirmation -> confirmation.getAppointment().getId()));
    }
}
