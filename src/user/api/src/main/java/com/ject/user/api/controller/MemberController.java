package com.ject.user.api.controller;

import com.ject.common.core.time.ServiceTime;
import com.ject.common.web.response.ApiResponse;
import com.ject.user.api.controller.api.MemberApi;
import com.ject.user.api.dto.enums.Gender;
import com.ject.user.api.dto.enums.NicknameUnavailableReason;
import com.ject.user.api.dto.enums.NoShowStatus;
import com.ject.user.api.dto.enums.SocialProvider;
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
import com.ject.user.application.dto.MarketingConsent;
import com.ject.user.application.dto.MyInfo;
import com.ject.user.application.dto.NicknameChange;
import com.ject.user.application.dto.NicknameAvailability;
import com.ject.user.application.service.MemberService;
import com.ject.user.application.service.NicknameService;
import com.ject.user.application.service.WithdrawalNoticeService;
import com.ject.user.application.service.WithdrawalService;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.ject.user.api.controller.UserStubData.MY_ID;
import static com.ject.user.api.controller.UserStubData.MY_NICKNAME;
import static com.ject.user.api.controller.UserStubData.NOW;

@RestController
public class MemberController implements MemberApi {

    private final NicknameService nicknameService;
    private final MemberService memberService;
    private final WithdrawalService withdrawalService;
    private final WithdrawalNoticeService withdrawalNoticeService;
    private final ServiceTime serviceTime;

    public MemberController(NicknameService nicknameService, MemberService memberService,
                            WithdrawalService withdrawalService, WithdrawalNoticeService withdrawalNoticeService,
                            ServiceTime serviceTime) {
        this.nicknameService = nicknameService;
        this.memberService = memberService;
        this.withdrawalService = withdrawalService;
        this.withdrawalNoticeService = withdrawalNoticeService;
        this.serviceTime = serviceTime;
    }

    @Override
    public ApiResponse<NicknameAvailabilityResponse> checkNickname(String nickname) {
        NicknameAvailability result = nicknameService.checkAvailability(nickname);
        NicknameUnavailableReason reason = result.unavailableReason() == null
                ? null
                : NicknameUnavailableReason.valueOf(result.unavailableReason());
        // 금지어 경고는 출시 후 (A-05-2 · P-02-06)
        return ApiResponse.success(new NicknameAvailabilityResponse(result.available(), reason, false));
    }

    @Override
    public ApiResponse<MyInfoResponse> getMe(Long memberId) {
        MyInfo info = memberService.getMyInfo(memberId);
        Gender gender = info.gender() == null ? null : Gender.valueOf(info.gender());
        // 매너 지표 · 노쇼 · 경고 · 이용 제한은 구현 전, 받은 후기는 출시 후
        return ApiResponse.success(new MyInfoResponse(info.userId(), info.nickname(),
                SocialProvider.valueOf(info.provider()), info.birthDate(), gender,
                info.completedCount(), serviceTime.toOffset(info.nicknameChangeableAt()), info.marketingAgreed(),
                serviceTime.toOffset(info.createdAt()), null, null, null, null, null));
    }

    @Override
    public ApiResponse<WithdrawalNoticeResponse> getWithdrawalNotice(Long memberId) {
        int count = withdrawalNoticeService.countInProgressExchanges(memberId);
        return ApiResponse.success(new WithdrawalNoticeResponse(count));
    }

    @Override
    public ApiResponse<Void> withdraw(Long memberId, WithdrawalRequest request) {
        String reason = request == null || request.reason() == null ? null : request.reason().name();
        withdrawalService.withdraw(memberId, reason);
        return ApiResponse.success(null);
    }

    @Override
    public ApiResponse<MarketingConsentResponse> updateMarketingConsent(Long memberId,
                                                                        MarketingConsentRequest request) {
        MarketingConsent consent = memberService.updateMarketingConsent(memberId, request.agreed());
        return ApiResponse.success(new MarketingConsentResponse(consent.agreed(),
                serviceTime.toOffset(consent.changedAt())));
    }

    @Override
    public ApiResponse<NicknameUpdateResponse> updateNickname(Long memberId, NicknameUpdateRequest request) {
        NicknameChange change = memberService.changeNickname(memberId, request.nickname());
        return ApiResponse.success(new NicknameUpdateResponse(change.nickname(),
                serviceTime.toOffset(change.nicknameChangeableAt())));
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
