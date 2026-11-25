package com.ject.exchange.application.dto.dto;

import com.ject.exchange.domain.domain.appointment.Appointment;
import com.ject.exchange.domain.domain.exchange.Exchange;
import com.ject.exchange.domain.domain.exchange.ExchangeStatus;

import java.time.Instant;
import java.util.Optional;

/**
 * 채팅 입력칸 + 메뉴의 약속 잡기 · 취소 상태 (E-01-2). 두 참여자에게 같다
 */
public enum AppointmentButton {
    /**
     * 약속 잡기를 쓸 수 있다
     */
    PROPOSE,
    /**
     * 약속 잡기가 보이지만 누를 수 없다 (같은 게시글이 다른 교환에 묶였거나 삭제됨, E-11-1)
     */
    PROPOSE_DISABLED,
    /**
     * 확정 약속이 있어 약속 취소가 보인다 (교환 확인 질문 전, E-07-1)
     */
    CANCEL,
    /**
     * 약속 잡기 · 취소 모두 없다 (교환 확인 질문 뒤 · 끝난 교환, E-01-2 · F-01-2)
     */
    NONE;

    /**
     * 교환 · 확정 약속 · 요청받은 게시글 상태로 버튼을 고른다 (spec 가정 1 우선순위)
     * @param post 요청받은 게시글 정보. 찾을 수 없으면 null
     * @param confirmed 이 교환의 확정(CONFIRMED) 약속
     * @param now 교환 확인 열림 판단 시각
     */
    public static AppointmentButton of(Exchange exchange, PostInfo post, Optional<Appointment> confirmed, Instant now) {
        if (exchange.getStatus() != ExchangeStatus.ACTIVE) {
            return NONE;
        }
        if (confirmed.isPresent()) {
            // 교환 확인 질문이 뜬 뒤에는 취소 대신 [교환하지 못했어요]로 정리하므로 + 메뉴에 사진 선택만 남는다 (E-01-2 · E-07-1)
            Instant opensAt = confirmed.get().confirmationOpensAt();
            return opensAt != null && !now.isBefore(opensAt) ? NONE : CANCEL;
        }
        // 같은 게시글이 다른 교환에 묶였거나(예약중 · 교환 완료) 삭제되면 약속 잡기를 막는다 (E-11-1 동작 1)
        if (post == null || post.status() != PostInfo.Status.AVAILABLE) {
            return PROPOSE_DISABLED;
        }
        return PROPOSE;
    }
}
