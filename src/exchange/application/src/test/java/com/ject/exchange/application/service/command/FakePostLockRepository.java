package com.ject.exchange.application.service.command;

import com.ject.exchange.domain.repository.PostLockRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 테스트용 가짜 글 잠금. 실제 잠금은 DB에서만 의미가 있으므로 잠근 글과 잠금 시점의 잠금 조회 횟수만 기록한다.
 */
class FakePostLockRepository implements PostLockRepository {

    private final FakeAppointmentRepository appointmentRepository;
    private final List<List<Long>> lockedPostIds = new ArrayList<>();
    private final List<Integer> lockedLookupCountsAtLock = new ArrayList<>();

    FakePostLockRepository(FakeAppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    @Override
    public void lockAll(Collection<Long> postIds) {
        lockedPostIds.add(List.copyOf(postIds));
        lockedLookupCountsAtLock.add(appointmentRepository.lockedLookupCount());
    }

    List<List<Long>> lockedPostIds() {
        return lockedPostIds;
    }

    /** 잠금을 잡을 때까지 약속을 잠금 조회한 횟수. 잠금이 먼저면 0이다. */
    List<Integer> lockedLookupCountsAtLock() {
        return lockedLookupCountsAtLock;
    }
}
