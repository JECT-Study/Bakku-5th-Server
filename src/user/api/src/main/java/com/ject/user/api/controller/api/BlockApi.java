package com.ject.user.api.controller.api;

import com.ject.common.web.response.ApiResponse;
import com.ject.user.api.dto.request.BlockRequest;
import com.ject.user.api.dto.response.BlockResponse;
import com.ject.user.api.dto.response.BlockedMemberResponse;
import com.ject.user.api.dto.response.CursorPageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name = "Block", description = "차단 · 차단 목록 · 차단 해제")
@RequestMapping("/api/v1/members/me/blocks")
public interface BlockApi {

    @Operation(summary = "BL-1 차단",
            description = "차단하면 서로의 글 · 채팅이 보이지 않고 상대는 내 글에 교환 요청을 할 수 없습니다. 예약중인 약속은 자동 취소됩니다. "
                    + "이미 차단한 회원이면 200 OK로 기존 차단 시각을 돌려줍니다. "
                    + "에러: MEMBER_401 자기 자신을 차단 · MEMBER_201 회원 없음 · 탈퇴한 회원")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<BlockResponse> block(@Valid @RequestBody BlockRequest request);

    @Operation(summary = "BL-2 차단 목록",
            description = "최근 차단한 순서로 보여줍니다. 그사이 탈퇴한 회원은 목록에서 뺍니다.")
    @GetMapping
    ApiResponse<CursorPageResponse<BlockedMemberResponse>> getBlocks(
            @Parameter(description = "이전 응답의 nextCursor. 첫 요청은 생략")
            @RequestParam(value = "cursor", required = false) String cursor,
            @Parameter(description = "1~50, 기본 20")
            @RequestParam(value = "size", defaultValue = "20") @Min(1) @Max(50) Integer size);

    @Operation(summary = "BL-3 차단 해제",
            description = "확인 팝업 없이 바로 해제하고 상대 글이 다시 보입니다. 차단으로 취소된 약속은 되살리지 않습니다. 차단하지 않은 회원이어도 성공으로 처리합니다.")
    @DeleteMapping("/{userId}")
    ApiResponse<Void> unblock(@Parameter(description = "차단 해제할 회원 ID") @PathVariable("userId") Long userId);
}
