package com.ject.post.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ject.post.api.dto.enums.PostCategory;
import com.ject.post.api.dto.enums.PostStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "교환글. 작성(PO-1) · 수정(PO-4) 응답에는 canRequest가 없음 (내 글)")
public record PostResponse(
        @Schema(description = "교환글 ID", example = "10")
        Long id,

        @Schema(description = "교환글 상태. 상세에서는 AVAILABLE, RESERVED, COMPLETED", example = "AVAILABLE")
        PostStatus status,

        @Schema(description = "사진 목록. 첫 장이 대표")
        List<ImageResponse> images,

        @Schema(description = "제품명", example = "치이카와 하치와레 가챠 키링")
        String title,

        @Schema(description = "카테고리", example = "CHARACTER")
        PostCategory category,

        @Schema(description = "개봉 여부", example = "false")
        boolean opened,

        @Schema(description = "태그. 없으면 빈 배열", example = "[\"치이카와\", \"하치와레\"]")
        List<String> tags,

        @Schema(description = "자세한 설명. 없으면 null", example = "치이카와 가챠 3탄에서 뽑았어요.")
        String description,

        @Schema(description = "원하는 아이템. null이면 '아무거나 상관없어요'", example = "치이카와 가챠 키링 (하치와레, 모몽가 제외)")
        String wantedItem,

        @Schema(description = "희망 장소")
        PlaceResponse place,

        @Schema(description = "교환 가능이고 종료 시각 전이면 true. 지금 교환 배지 표시 기준", example = "true")
        boolean nowExchange,

        @Schema(description = "지금 교환 종료 시각. 예약중이어도 값 유지. 일반 교환이면 null. 남은 시간은 FE 계산", example = "2026-11-02T15:30:00+09:00")
        OffsetDateTime nowExchangeEndsAt,

        @Schema(description = "나가지 않은 채팅방 수", example = "3")
        int requestCount,

        @Schema(description = "조회 수. 다른 사람 글을 열 때 계정 + 날짜당 1회 증가", example = "41")
        int viewCount,

        @Schema(description = "게시자")
        AuthorResponse author,

        @Schema(description = "내 글 여부", example = "false")
        boolean isMine,

        @Schema(description = "내가 이 글로 채팅 중인 방 ID. 없거나 내가 나간 방이면 null", example = "5")
        Long chatRoomId,

        @Schema(description = "교환 요청 가능 여부. 상세(PO-3)에만 있음. 내 글, 예약중 · 교환 완료, 내가 나간 채팅방이 있는 글이면 false", example = "true")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Boolean canRequest,

        @Schema(description = "작성 시각. 상대 시간은 FE 계산", example = "2026-11-02T14:30:00+09:00")
        OffsetDateTime createdAt
) {
}
