package com.ject.user.api.dto.response;

import com.ject.user.api.dto.enums.NoShowStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "노쇼 기록 (2차)")
public record NoShowResponse(
        @Schema(description = "노쇼 기록 ID", example = "7")
        Long noShowId,

        @Schema(description = "약속 일시", example = "2026-12-20T15:00:00+09:00")
        OffsetDateTime scheduledAt,

        @Schema(description = "약속 장소 이름", example = "홍대입구역 9번 출구")
        String placeName,

        @Schema(description = "교환 상대 닉네임. 탈퇴 시 '탈퇴한 사용자'", example = "하치와레러버")
        String counterpartNickname,

        @Schema(description = "처리 상태", example = "PENDING")
        NoShowStatus status
) {
}
