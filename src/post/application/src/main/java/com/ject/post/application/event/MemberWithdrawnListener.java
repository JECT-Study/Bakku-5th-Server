package com.ject.post.application.event;

import com.ject.common.event.member.MemberWithdrawn;
import com.ject.post.application.service.command.WithdrawnMemberPostService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class MemberWithdrawnListener {

    private static final Logger log = LoggerFactory.getLogger(MemberWithdrawnListener.class);

    private final WithdrawnMemberPostService withdrawnMemberPostService;

    @TransactionalEventListener
    public void handle(MemberWithdrawn event) {
        try {
            int deleted = withdrawnMemberPostService.deleteAvailablePosts(event.memberId(), event.withdrawnAt());
            log.info("탈퇴 회원 교환글 삭제. memberId={}, deleted={}", event.memberId(), deleted);
        } catch (RuntimeException e) {
            log.error("탈퇴 회원 교환글 삭제 실패. memberId={}", event.memberId(), e);
        }
    }
}
