package com.ject.exchange.domain.repository;

import com.ject.exchange.domain.domain.appointment.Appointment;
import com.ject.exchange.domain.domain.appointment.AppointmentStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface AppointmentRepository {

    Appointment save(Appointment appointment);

    Optional<Appointment> findById(Long id);

    List<Appointment> findAllByExchangeId(Long exchangeId);

    /**
     * 같은 글의 다른 교환에 수락을 기다리는 제안(PROPOSED)이 있는지. 자동 만료가 없어 시각은 보지 않는다 (E-10-1 출시 후)
     * @param postId 요청받은 교환글 ID
     * @param excludedExchangeId 제외할 교환 ID (지금 보고 있는 교환)
     */
    boolean existsPendingProposalOnPost(Long postId, Long excludedExchangeId);

    /**
     * 약속을 잠그고 조회한다 (수락처럼 상태를 바꾸는 처리용). 같은 트랜잭션에서 처음 읽어야 최신 상태가 보인다
     */
    Optional<Appointment> findByIdForUpdate(Long id);

    /**
     * 약속에 걸린 글 ID: 교환의 글과 약속이 고른 첨부 글 (중복 없음). 약속이 없으면 빈 목록
     */
    List<Long> findPostIdsOf(Long appointmentId);

    /**
     * 주어진 글 중 다른 교환의 해당 상태 약속에 묶인 글 ID.
     * "묶임" = 그 약속의 교환 글이거나, 그 약속이 고른 첨부 글
     * @param excludedExchangeId 제외할 교환 ID (지금 처리 중인 교환)
     */
    Set<Long> findPostIdsBoundBy(Collection<Long> postIds, AppointmentStatus status, Long excludedExchangeId);

    /**
     * 주어진 글에 걸린 다른 교환의 수락을 기다리는 제안(PROPOSED). 자동 만료가 없어 시각은 보지 않는다.
     * "걸림" = 그 교환의 글이 주어진 글이거나, 그 제안이 고른 첨부 글이 주어진 글. 교환 상태는 보지 않는다
     */
    List<Appointment> findPendingProposalsInvolving(Collection<Long> postIds, Long excludedExchangeId);

    /**
     * 회원이 제안자(proposerUserId) 또는 받는 사람(receiverUserId)으로 참여한 현재 CONFIRMED 약속 수
     */
    int countConfirmedByMemberId(Long memberId);

    /**
     * 회원이 제안자 또는 받는 사람으로 참여하고 상태가 주어진 목록에 속하는 약속 ID를 오름차순으로 반환한다.
     * 엔티티를 미리 영속성 컨텍스트에 올리지 않아 이후 findByIdForUpdate가 최신 상태를 읽게 한다
     */
    List<Long> findIdsByMemberIdAndStatuses(Long memberId, Collection<AppointmentStatus> statuses);
}
