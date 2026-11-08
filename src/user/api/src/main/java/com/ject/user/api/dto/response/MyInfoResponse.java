package com.ject.user.api.dto.response;

import com.ject.user.api.dto.enums.Gender;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Schema(description = "내 정보. 1차는 마이페이지 요약(닉네임 · 교환 완료 횟수)에 사용, 2차 필드는 1차에 항상 null")
public record MyInfoResponse(
        @Schema(description = "회원 ID", example = "101")
        Long userId,

        @Schema(description = "닉네임", example = "쿠로미덕후")
        String nickname,

        @Schema(description = "생년월일 (본인만, 읽기 전용)", example = "2001-03-15")
        LocalDate birthDate,

        @Schema(description = "성별 (본인만, 읽기 전용). 가입 때 비워 뒀으면 null", example = "FEMALE", nullable = true)
        Gender gender,

        @Schema(description = "교환 완료 횟수. 0이면 FE가 '첫 교환을 기다리고 있어요'", example = "12")
        int completedCount,

        @Schema(description = "다음 닉네임 변경 가능 시각. null이면 지금 변경 가능 (가입 직후 포함)", nullable = true)
        OffsetDateTime nicknameChangeableAt,

        @Schema(description = "마케팅 정보 수신 동의 여부 (MB-7)", example = "false")
        boolean marketingAgreed,

        @Schema(description = "가입 시각", example = "2026-11-27T10:00:00+09:00")
        OffsetDateTime createdAt,

        @Schema(description = "매너 지표. 2차, 1차는 항상 null. 교환 0회면 null", nullable = true)
        Double mannerScore,

        @Schema(description = "받은 후기 단계별 개수. 2차, 1차는 null", nullable = true)
        ReceivedReviewsResponse receivedReviews,

        @Schema(description = "노쇼 기록 요약. 2차, 1차는 null", nullable = true)
        NoShowSummaryResponse noShow,

        @Schema(description = "위반 경고 횟수. 2차, 1차는 null", nullable = true)
        Integer warningCount,

        @Schema(description = "이용 제한 해제 일시. 제한 중이 아니면 null. 2차", nullable = true)
        OffsetDateTime restrictedUntil
) {
}
