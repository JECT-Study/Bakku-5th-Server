package com.ject.exchange.api.dto.response;

import com.ject.common.web.image.ImageUrlResolver;
import com.ject.exchange.api.dto.enums.AppointmentButton;
import com.ject.exchange.application.dto.dto.ExchangeResult;
import com.ject.exchange.domain.domain.exchange.ExchangeStatus;
import com.ject.exchange.domain.domain.exchange.OfferType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

@Schema(description = "교환")
public record ExchangeResponse(
        @Schema(description = "교환 ID", example = "1")
        Long id,

        @Schema(description = "교환 상태", example = "ACTIVE")
        ExchangeStatus status,

        @Schema(description = "요청한 교환글의 현재 정보 (상단 고정 카드)")
        PostSummaryResponse post,

        @Schema(description = "요청자")
        ParticipantResponse requester,

        @Schema(description = "교환글 게시자")
        ParticipantResponse owner,

        @Schema(description = "첨부 방식", example = "POST")
        OfferType offerType,

        @Schema(description = "첨부 아이템. POST면 1~5개, NEW면 1개")
        List<OfferedItemResponse> offeredItems,

        @Schema(description = "이 교환의 채팅방 ID", example = "5")
        Long chatRoomId,

        @Schema(description = "입력칸 + 메뉴의 약속 잡기 · 취소. PROPOSE 약속 잡기 / PROPOSE_DISABLED 보이지만 누를 수 없음(같은 게시글이 다른 곳에 묶임 · 삭제) / CANCEL 약속 취소(확정 약속, 교환 확인 전) / NONE 둘 다 없음(교환 확인 질문 뒤 · 끝난 교환)", example = "PROPOSE")
        AppointmentButton appointmentButton,

        @Schema(description = "요청 시각", example = "2026-10-26T14:30:00+09:00")
        OffsetDateTime createdAt
) {
    @Builder
    public ExchangeResponse {
    }

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    public static ExchangeResponse from(ExchangeResult result, ImageUrlResolver imageUrlResolver) {
        return ExchangeResponse.builder()
                .id(result.id())
                .status(result.status())
                .post(PostSummaryResponse.from(result.post(), imageUrlResolver))
                .requester(ParticipantResponse.from(result.requester()))
                .owner(ParticipantResponse.from(result.owner()))
                .offerType(result.offerType())
                .offeredItems(result.offeredItems().stream()
                        .map(item -> OfferedItemResponse.from(item, imageUrlResolver))
                        .toList())
                .chatRoomId(result.chatRoomId())
                .appointmentButton(EnumMapper.map(result.appointmentButton(), AppointmentButton.class))
                .createdAt(result.createdAt() == null ? null : result.createdAt().atZone(SEOUL).toOffsetDateTime())
                .build();
    }
}
