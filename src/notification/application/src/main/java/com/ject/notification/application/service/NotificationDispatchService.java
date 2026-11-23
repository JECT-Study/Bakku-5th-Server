package com.ject.notification.application.service;

import com.ject.common.event.notification.NotificationRequested;
import com.ject.common.event.notification.NotificationTargetType;
import com.ject.notification.application.port.MemberNicknamePort;
import com.ject.notification.application.port.NotificationRepository;
import com.ject.notification.domain.domain.Notification;
import com.ject.notification.domain.domain.NotificationContent;
import com.ject.notification.domain.domain.NotificationMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 다른 파트의 알림 요청을 받는 사람별 알림으로 만든다. 문장 · 저장 여부는 알림 쪽이 정한다 (결정 B2).
 */
@Service
public class NotificationDispatchService {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatchService.class);

    private final NotificationRepository notificationRepository;
    private final MemberNicknamePort memberNicknamePort;

    public NotificationDispatchService(NotificationRepository notificationRepository,
                                       MemberNicknamePort memberNicknamePort) {
        this.notificationRepository = notificationRepository;
        this.memberNicknamePort = memberNicknamePort;
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
        List<Long> receiverIds = receiversOf(request);
        if (receiverIds.isEmpty()) {
            return;
        }
        NotificationContent content = NotificationMessages.content(request.type(), senderNickname(request.senderId()),
                request.params().get(NotificationMessages.PRODUCT_NAME));
        if (NotificationMessages.savesToList(request.type())) {
            notificationRepository.saveAll(receiverIds.stream()
                    .map(receiverId -> toNotification(receiverId, request, content))
                    .toList());
        }
    }

    /**
     * 보낸 회원 자신은 받지 않는다.
     */
    private static List<Long> receiversOf(NotificationRequested request) {
        return request.receiverIds().stream()
                .filter(receiverId -> !receiverId.equals(request.senderId()))
                .distinct()
                .toList();
    }

    private String senderNickname(Long senderId) {
        return senderId == null ? null : memberNicknamePort.getNickname(senderId);
    }

    private static Notification toNotification(Long receiverId, NotificationRequested request,
                                               NotificationContent content) {
        NotificationRequested.Target target = request.target();
        NotificationTargetType targetType = target == null ? null : target.type();
        Long targetId = target == null ? null : target.id();
        return Notification.create(receiverId, request.type(), content, targetType, targetId);
    }
}
