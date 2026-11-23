package com.ject.notification.domain.domain;

import com.ject.common.event.notification.NotificationType;

import java.util.EnumMap;
import java.util.Map;

/**
 * 알림 종류별 문장과 알림 목록 저장 여부 (X-01-1 · P-07-04).
 * 문장은 PD 확정 전이라 기획 표현으로 두고, 확정되면 여기만 바꾼다.
 */
public final class NotificationMessages {

    /** 문장 뒤에 붙일 교환글 제품명의 params 키 */
    public static final String PRODUCT_NAME = "productName";

    /** 보낸 회원이 없는 알림의 제목 */
    public static final String SYSTEM_TITLE = "바꾸";

    private static final Map<NotificationType, String> SENTENCES = new EnumMap<>(NotificationType.class);

    static {
        SENTENCES.put(NotificationType.EXCHANGE_REQUESTED, "교환 요청이 왔어요");
        SENTENCES.put(NotificationType.CHAT_MESSAGE, "새 메시지가 왔어요");
        SENTENCES.put(NotificationType.APPOINTMENT_PROPOSED, "약속을 제안했어요");
        SENTENCES.put(NotificationType.APPOINTMENT_CONFIRMED, "약속이 확정됐어요");
        SENTENCES.put(NotificationType.APPOINTMENT_REJECTED, "약속 제안을 거절했어요");
        SENTENCES.put(NotificationType.APPOINTMENT_CANCELED, "약속이 취소됐어요");
        SENTENCES.put(NotificationType.APPOINTMENT_REOPENED, "다시 약속을 잡을 수 있어요");
        SENTENCES.put(NotificationType.APPOINTMENT_PROPOSAL_AUTO_CANCELED, "보낸 약속 제안이 취소됐어요");
        SENTENCES.put(NotificationType.EXCHANGE_CONFIRMATION, "교환은 잘 하셨나요?");
        SENTENCES.put(NotificationType.POST_UPDATED, "교환글 정보가 바뀌었어요");
        SENTENCES.put(NotificationType.POST_DELETED, "교환글이 삭제됐어요");
        SENTENCES.put(NotificationType.ATTACHED_ITEM_DELETED, "요청에 첨부한 아이템이 삭제됐어요");
        SENTENCES.put(NotificationType.APPOINTMENT_REMINDER, "30분 뒤에 약속이 있어요");
        SENTENCES.put(NotificationType.EXCHANGE_CONFIRMATION_REMINDER, "교환했는지 아직 알려 주지 않았어요");
        SENTENCES.put(NotificationType.AUTO_COMPLETE_SOON, "내일 교환이 자동으로 완료돼요");
        SENTENCES.put(NotificationType.AUTO_COMPLETED, "교환이 자동으로 완료됐어요");
    }

    private NotificationMessages() {
    }

    /**
     * 문장이 정해진 종류인지. 운영 안내 · 출시 후 알림은 아직 보내지 않는다.
     */
    public static boolean isSupported(NotificationType type) {
        return SENTENCES.containsKey(type);
    }

    /**
     * 채팅 메시지는 푸시로만 보내고 채팅 목록의 안 읽은 수로 확인한다 (X-01-1).
     */
    public static boolean savesToList(NotificationType type) {
        return type != NotificationType.CHAT_MESSAGE;
    }

    /**
     * @param senderNickname 보낸 회원 닉네임, 보낸 회원이 없으면 null
     * @param productName    교환글 제품명, 없으면 null
     * @throws IllegalArgumentException 문장이 정해지지 않은 종류일 때
     */
    public static NotificationContent content(NotificationType type, String senderNickname, String productName) {
        String sentence = SENTENCES.get(type);
        if (sentence == null) {
            throw new IllegalArgumentException("문장이 정해지지 않은 알림 종류: " + type);
        }
        String title = senderNickname == null ? SYSTEM_TITLE : senderNickname;
        String body = productName == null || productName.isBlank() ? sentence : "[" + productName + "] " + sentence;
        return new NotificationContent(title, body);
    }
}
