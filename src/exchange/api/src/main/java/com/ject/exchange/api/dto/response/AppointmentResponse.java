package com.ject.exchange.api.dto.response;

import com.ject.exchange.api.dto.enums.AppointmentStatus;
import com.ject.exchange.api.dto.enums.AppointmentTimeType;
import com.ject.exchange.application.dto.dto.AppointmentResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

@Schema(description = "약속")
public record AppointmentResponse(
        @Schema(description = "약속 ID", example = "3")
        Long id,

        @Schema(description = "교환 ID", example = "1")
        Long exchangeId,

        @Schema(description = "이 약속을 제안한 사람", example = "101")
        Long proposerUserId,

        @Schema(description = "제안받은 사람. 수락 · 거절 권한", example = "202")
        Long receiverUserId,

        @Schema(description = "약속 상태", example = "PROPOSED")
        AppointmentStatus status,

        @Schema(description = "시간 유형", example = "SCHEDULED")
        AppointmentTimeType timeType,

        @Schema(description = "약속 시각 (10분 단위)", example = "2026-10-27T15:00:00+09:00")
        OffsetDateTime scheduledAt,

        @Schema(description = "약속 장소. 합의 시점 값으로 고정")
        PlaceResponse place,

        @Schema(description = "교환할 물건 (첨부 아이템 중 고른 것)")
        List<AppointmentItemResponse> items,

        @Schema(description = "제안 만료 시각. PROPOSED가 아니면 null", example = "2026-10-27T15:00:00+09:00")
        OffsetDateTime expiresAt,

        @Schema(description = "내 약속 전 알림(분). null이면 끔. 지금 바로 약속은 항상 null", example = "30")
        Integer myReminderOffsetMinutes,

        @Schema(description = "같은 글의 다른 채팅방에 진행 중인 제안이 있는지. true면 수락 전 확인 창", example = "true")
        boolean otherActiveProposalsOnPost,

        @Schema(description = "대기 중인 변경 제안. 없으면 null")
        AppointmentChangeResponse pendingChange,

        @Schema(description = "처리 대기 중인 취소 요청 (REQUESTED 또는 REJECTED). 없으면 null")
        CancellationResponse pendingCancellation,

        @Schema(description = "교환 확인 정보. 확정 전이면 null")
        ConfirmationResponse confirmation,

        @Schema(description = "확정 시각. 확정 전이면 null", example = "2026-10-26T17:00:00+09:00")
        OffsetDateTime confirmedAt,

        @Schema(description = "제안 시각", example = "2026-10-26T16:40:00+09:00")
        OffsetDateTime createdAt
) {
    /**
     * 약속 제안 응답. 변경 제안 · 취소 요청 · 교환 확인은 아직 없으므로 null
     */
    public static AppointmentResponse from(AppointmentResult result) {
        return new AppointmentResponse(
                result.id(),
                result.exchangeId(),
                result.proposerUserId(),
                result.receiverUserId(),
                EnumMapper.map(result.status(), AppointmentStatus.class),
                EnumMapper.map(result.timeType(), AppointmentTimeType.class),
                toSeoul(result.scheduledAt()),
                new PlaceResponse(result.place().name(), result.place().address(), result.place().lat(), result.place().lng()),
                result.items().stream()
                        .map(item -> new AppointmentItemResponse(item.offeredItemId(), item.type(), item.name(), item.thumbnailUrl()))
                        .toList(),
                toSeoul(result.expiresAt()),
                result.myReminderOffsetMinutes(),
                result.otherActiveProposalsOnPost(),
                null,   // Todo: 약속 변경 제안 구현 후 반영
                null,   // Todo: 약속 취소 구현 후 반영
                null,   // Todo: 교환 확인 구현 후 반영
                toSeoul(result.confirmedAt()),
                toSeoul(result.createdAt()));
    }

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private static OffsetDateTime toSeoul(Instant instant) {
        return instant == null ? null : instant.atZone(SEOUL).toOffsetDateTime();
    }
}
