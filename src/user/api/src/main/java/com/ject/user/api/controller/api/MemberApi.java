package com.ject.user.api.controller.api;

import com.ject.common.web.response.ApiResponse;
import com.ject.user.api.dto.request.MarketingConsentRequest;
import com.ject.user.api.dto.request.NicknameUpdateRequest;
import com.ject.user.api.dto.request.WithdrawalRequest;
import com.ject.user.api.dto.response.CursorPageResponse;
import com.ject.user.api.dto.response.MarketingConsentResponse;
import com.ject.user.api.dto.response.MemberProfileResponse;
import com.ject.user.api.dto.response.MyInfoResponse;
import com.ject.user.api.dto.response.NicknameAvailabilityResponse;
import com.ject.user.api.dto.response.NicknameUpdateResponse;
import com.ject.user.api.dto.response.NoShowResponse;
import com.ject.user.api.dto.response.WithdrawalNoticeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Member", description = "닉네임 확인 · 내 정보 · 탈퇴 · 마케팅 수신 동의 · (2차) 닉네임 수정 · 다른 회원 프로필 · 노쇼 기록")
@RequestMapping("/api/v1/members")
public interface MemberApi {

    @Operation(summary = "MB-1 닉네임 사용 가능 확인",
            description = "형식 · 중복을 검사합니다. 사용할 수 없으면 에러 대신 available=false와 이유를 줍니다. FE는 입력을 멈추고 0.5초 뒤에 호출합니다. "
                    + "가입 중에도 쓰므로 인증 없이 호출하고 IP당 분당 30회로 제한합니다. stub은 '쿠로미덕후'(대소문자 무시)를 중복으로 봅니다. "
                    + "에러: COMMON_003 요청이 너무 많음")
    @SecurityRequirements
    @GetMapping("/nickname-availability")
    ApiResponse<NicknameAvailabilityResponse> checkNickname(
            @Parameter(description = "검사할 닉네임. 앞뒤 공백은 FE가 지우고 보냄", example = "쿠로미덕후")
            @RequestParam("nickname") String nickname);

    @Operation(summary = "MB-2 내 정보",
            description = "1차는 마이페이지 요약의 닉네임 · 교환 완료 횟수에 씁니다. 2차 필드(mannerScore · receivedReviews · noShow · warningCount · restrictedUntil)는 1차에 null입니다. "
                    + "에러: MEMBER_201 회원 없음 → 기기 로그인 정보 삭제 후 로그인 화면")
    @GetMapping("/me")
    ApiResponse<MyInfoResponse> getMe();

    @Operation(summary = "MB-5 탈퇴 전 안내 정보",
            description = "탈퇴 화면에 들어올 때 부릅니다. 진행 중인 교환이 있으면 FE가 안내 문구를 보여줍니다. 탈퇴는 막지 않습니다.")
    @GetMapping("/me/withdrawal-notice")
    ApiResponse<WithdrawalNoticeResponse> getWithdrawalNotice();

    @Operation(summary = "MB-6 회원 탈퇴",
            description = "진행 중인 교환이 있어도 탈퇴할 수 있습니다. 확정 약속 자동 취소 → 교환 가능 글 삭제 → 모든 채팅방 나감 순서로 처리하고, "
                    + "닉네임 · 생년월일 · 성별은 즉시 비웁니다. 소셜 연결 끊기, 리프레시 · 푸시 토큰 삭제도 함께 합니다.")
    @DeleteMapping("/me")
    ApiResponse<Void> withdraw(@Valid @RequestBody(required = false) WithdrawalRequest request);

    @Operation(summary = "MB-7 마케팅 수신 동의 변경",
            description = "설정 > 알림 설정의 마케팅 정보 수신 토글입니다. 1차 알림 설정은 이것만 있습니다. 바꾸면 변경 일시를 저장합니다.")
    @PutMapping("/me/marketing-consent")
    ApiResponse<MarketingConsentResponse> updateMarketingConsent(@Valid @RequestBody MarketingConsentRequest request);

    @Operation(summary = "MB-3 닉네임 수정 (2차)",
            description = "닉네임은 30일에 한 번 바꿀 수 있습니다. 가입 때 정한 닉네임은 횟수에 넣지 않아 가입 직후 1회 바꿀 수 있습니다. "
                    + "에러: MEMBER_101 형식 오류 · MEMBER_102 중복 · MEMBER_103 30일 안에 다시 변경 · MEMBER_104 지금 닉네임과 같음")
    @PatchMapping("/me/nickname")
    ApiResponse<NicknameUpdateResponse> updateNickname(@Valid @RequestBody NicknameUpdateRequest request);

    @Operation(summary = "MB-4 다른 회원 프로필 (2차)",
            description = "게시자 프로필에 씁니다. 닉네임과 교환 완료 횟수만 공개하고 생년월일 · 성별 · 노쇼 · 후기는 공개하지 않습니다. "
                    + "에러: MEMBER_201 회원 없음 · MEMBER_202 탈퇴한 회원 · MEMBER_203 차단 관계")
    @GetMapping("/{userId}")
    ApiResponse<MemberProfileResponse> getProfile(@Parameter(description = "회원 ID") @PathVariable("userId") Long userId);

    @Operation(summary = "MB-8 내 노쇼 기록 (2차)",
            description = "본인만 보는 노쇼 기록입니다. 건마다 약속 일시 · 장소 · 상대 닉네임 · 처리 상태를 보여줍니다. 신고자가 쓴 추가 설명은 보여주지 않습니다.")
    @GetMapping("/me/no-shows")
    ApiResponse<CursorPageResponse<NoShowResponse>> getNoShows(
            @Parameter(description = "이전 응답의 nextCursor. 첫 요청은 생략")
            @RequestParam(value = "cursor", required = false) String cursor,
            @Parameter(description = "1~50, 기본 20")
            @RequestParam(value = "size", defaultValue = "20") @Min(1) @Max(50) Integer size);
}
