package com.ject.user.api.controller;

import com.ject.common.core.time.ServiceTime;
import com.ject.common.web.response.ApiResponse;
import com.ject.user.api.controller.api.MemberApi;
import com.ject.user.api.dto.enums.Gender;
import com.ject.user.api.dto.enums.NicknameUnavailableReason;
import com.ject.user.api.dto.enums.NoShowStatus;
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
import com.ject.user.application.dto.MyInfo;
import com.ject.user.application.dto.NicknameAvailability;
import com.ject.user.application.service.MemberService;
import com.ject.user.application.service.NicknameService;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.ject.user.api.controller.UserStubData.MY_ID;
import static com.ject.user.api.controller.UserStubData.MY_NICKNAME;
import static com.ject.user.api.controller.UserStubData.NOW;

@RestController
public class MemberController implements MemberApi {

    private final NicknameService nicknameService;
    private final MemberService memberService;
    private final ServiceTime serviceTime;

    public MemberController(NicknameService nicknameService, MemberService memberService, ServiceTime serviceTime) {
        this.nicknameService = nicknameService;
        this.memberService = memberService;
        this.serviceTime = serviceTime;
    }

    @Override
    public ApiResponse<NicknameAvailabilityResponse> checkNickname(String nickname) {
        NicknameAvailability result = nicknameService.checkAvailability(nickname);
        NicknameUnavailableReason reason = result.unavailableReason() == null
                ? null
                : NicknameUnavailableReason.valueOf(result.unavailableReason());
        // 금지어 경고는 2차 (A-05-2)
        return ApiResponse.success(new NicknameAvailabilityResponse(result.available(), reason, false));
    }

    @Override
    public ApiResponse<MyInfoResponse> getMe(Long memberId) {
        MyInfo info = memberService.getMyInfo(memberId);
        Gender gender = info.gender() == null ? null : Gender.valueOf(info.gender());
        // 매너 지표 · 후기 · 노쇼 · 경고 · 이용 제한은 2차 (MVP v0.9)
        return ApiResponse.success(new MyInfoResponse(info.userId(), info.nickname(), info.birthDate(), gender,
                info.completedCount(), serviceTime.toOffset(info.nicknameChangeableAt()), info.marketingAgreed(),
                serviceTime.toOffset(info.createdAt()), null, null, null, null, null));
    }

    @Override
    public ApiResponse<WithdrawalNoticeResponse> getWithdrawalNotice() {
        return ApiResponse.success(new WithdrawalNoticeResponse(1));
    }

    @Override
    public ApiResponse<Void> withdraw(WithdrawalRequest request) {
        return ApiResponse.success(null);
    }

    @Override
    public ApiResponse<MarketingConsentResponse> updateMarketingConsent(MarketingConsentRequest request) {
        return ApiResponse.success(new MarketingConsentResponse(request.agreed(), NOW));
    }

    @Override
    public ApiResponse<NicknameUpdateResponse> updateNickname(NicknameUpdateRequest request) {
        return ApiResponse.success(new NicknameUpdateResponse(request.nickname(), NOW.plusDays(30)));
    }

    @Override
    public ApiResponse<MemberProfileResponse> getProfile(Long userId) {
        if (MY_ID.equals(userId)) {
            return ApiResponse.success(new MemberProfileResponse(MY_ID, MY_NICKNAME, 12, true, null));
        }
        return ApiResponse.success(new MemberProfileResponse(userId, "하치와레러버", 5, false, null));
    }

    @Override
    public ApiResponse<CursorPageResponse<NoShowResponse>> getNoShows(String cursor, Integer size) {
        return ApiResponse.success(new CursorPageResponse<>(List.of(
                new NoShowResponse(7L, NOW.plusDays(23), "홍대입구역 9번 출구", "하치와레러버", NoShowStatus.PENDING)), null, false));
    }
}
