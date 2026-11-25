package com.ject.user.api.controller.api;

import com.ject.common.web.auth.LoginMember;
import com.ject.common.web.response.ApiResponse;
import com.ject.user.api.dto.request.ReportRequest;
import com.ject.user.api.dto.response.ReportResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name = "Report", description = "신고")
@RequestMapping("/api/v1/reports")
public interface ReportApi {

    @Operation(summary = "RP-1 신고하기",
            description = "교환글 · 게시자 프로필 · 요청 아이템 상세 · 채팅방 · 교환 내역의 공통 신고 화면에서 사유를 골라 신고합니다. 같은 대상은 한 번만 신고할 수 있고, "
                    + "신고자는 상대에게 공개하지 않으며 처리 결과도 알리지 않습니다. 운영자가 확인해 처리합니다. "
                    + "지금은 회원 신고만 대상이 있는지 확인하고, 채팅방은 채팅방 ID 기준으로 한 번만 받습니다. "
                    + "에러: REPORT_001 신고 대상 없음(지금은 회원 신고만) · REPORT_002 이미 신고함 · REPORT_004 나 자신 신고 · "
                    + "COMMON_001 기타인데 내용 없음 · 300자 초과 · CHAT_MESSAGE(지원하지 않음)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<ReportResponse> report(@LoginMember Long memberId, @Valid @RequestBody ReportRequest request);
}
