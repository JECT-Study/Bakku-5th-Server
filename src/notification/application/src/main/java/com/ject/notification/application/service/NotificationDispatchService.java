package com.ject.notification.application.service;

import com.ject.common.event.notification.NotificationRequested;
import com.ject.common.event.notification.NotificationTargetType;
import com.ject.common.event.notification.NotificationType;
import com.ject.notification.application.dto.PushMessage;
import com.ject.notification.application.port.BlockRelationPort;
import com.ject.notification.application.port.MemberNicknamePort;
import com.ject.notification.application.port.NotificationRepository;
import com.ject.notification.application.port.PushSender;
import com.ject.notification.application.port.PushTokenPort;
import com.ject.notification.domain.domain.Notification;
import com.ject.notification.domain.domain.NotificationContent;
import com.ject.notification.domain.domain.NotificationMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * 다른 파트의 알림 요청을 받는 사람별 알림으로 만들어 저장하고 푸시를 보낸다. 문장 · 저장 여부는 알림 쪽이 정한다 (결정 B2).
 */
@Service
public class NotificationDispatchService {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatchService.class);

    private final NotificationRepository notificationRepository;
    private final MemberNicknamePort memberNicknamePort;
    private final BlockRelationPort blockRelationPort;
    private final PushTokenPort pushTokenPort;
    private final PushSender pushSender;

    public NotificationDispatchService(NotificationRepository notificationRepository,
                                       MemberNicknamePort memberNicknamePort, BlockRelationPort blockRelationPort,
                                       PushTokenPort pushTokenPort, PushSender pushSender) {
        this.notificationRepository = notificationRepository;
        this.memberNicknamePort = memberNicknamePort;
        this.blockRelationPort = blockRelationPort;
        this.pushTokenPort = pushTokenPort;
        this.pushSender = pushSender;
    }

    /**
     * 요청한 쪽 트랜잭션이 커밋된 뒤 불리므로 새 트랜잭션을 연다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void dispatch(NotificationRequested request) {
        if (!NotificationMessages.isSupported(request.type())) {
            log.warn("아직 보내지 않는 알림 종류라 건너뜀. type={}", request.type());
            return;
        }
        if (request.senderId() == null) {
            log.warn("제목에 넣을 상대방(senderId)이 없어 건너뜀. type={}, receiverIds={}", request.type(),
                    request.receiverIds());
            return;
        }
        List<Long> receiverIds = receiversOf(request);
        if (receiverIds.isEmpty()) {
            return;
        }
        NotificationContent content = NotificationMessages.content(request.type(),
                memberNicknamePort.getNickname(request.senderId()),
                request.params().get(NotificationMessages.PRODUCT_NAME));
        NotificationTargetType targetType = request.target() == null ? null : request.target().type();
        Long targetId = request.target() == null ? null : request.target().id();

        if (NotificationMessages.savesToList(request.type())) {
            notificationRepository.saveAll(receiverIds.stream()
                    .map(receiverId -> Notification.create(receiverId, request.type(), content, targetType, targetId))
                    .toList());
        }
        push(receiverIds, new PushMessage(request.type(), content.title(), content.body(), targetType, targetId));
    }

    /**
     * 보낸 회원 자신과 보낸 회원과 차단 관계인 회원은 받지 않는다.
     * 약속 취소는 차단으로 확정 약속이 취소될 때도 보내야 해서 차단을 확인하지 않는다 (X-01-1, 결정 A13).
     */
    private List<Long> receiversOf(NotificationRequested request) {
        Long senderId = request.senderId();
        Set<Long> blocked = request.type() == NotificationType.APPOINTMENT_CANCELED
                ? Set.of()
                : Set.copyOf(blockRelationPort.getBlockRelatedMemberIds(senderId));
        return request.receiverIds().stream()
                .filter(receiverId -> !receiverId.equals(senderId) && !blocked.contains(receiverId))
                .distinct()
                .toList();
    }

    /**
     * 푸시가 실패해도 알림 목록 저장은 남긴다.
     */
    private void push(List<Long> receiverIds, PushMessage message) {
        try {
            List<String> pushTokens = pushTokenPort.getPushTokens(receiverIds);
            if (!pushTokens.isEmpty()) {
                pushSender.send(pushTokens, message);
            }
        } catch (RuntimeException e) {
            log.warn("푸시 발송 실패. type={}, receiverIds={}", message.type(), receiverIds, e);
        }
    }
}
