package com.ject.user.api.controller.api;

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
                    + "신고자는 상대에게 공개하지 않으며 처리 결과도 알리지 않습니다. 1차는 개발자가 DB에서 확인해 PM에게 전달합니다. "
                    + "에러: REPORT_001 신고 대상 없음 · REPORT_002 이미 신고함 · REPORT_003 교환 물품 문제 사유를 교환 완료 상대가 아닌 대상에 사용 · REPORT_004 내 글 · 나 자신 신고")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<ReportResponse> report(@Valid @RequestBody ReportRequest request);
}
